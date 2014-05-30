# coding=utf8
import urllib2, json, time
from weather import Weather

def translate_icon(icon):
    trtable = {
        '01': 'skc_',
        '02': 'bkn_',
        '03': 'ovc',
        '04': 'ovc',
        '09': 'ovc_+ra',
        '10': 'bkn_ra_',
        '11': 'ovc_ts_ra',
        '13': 'ovc_+sn',
        '50': 'fg_'
    }
    s = trtable[icon[:-1]]
    if s[-1] == '_': s += icon[-1]
    return s

class OWMWeather(Weather):
    def __init__(self, city):
        self.cityid = city.encode('utf8')
        self.city = city

    def get(self):
        url = 'http://api.openweathermap.org/data/2.5/weather?q=%s&lang=ru&units=metric' % urllib2.quote(self.cityid)
        resp = json.loads( urllib2.urlopen(url).read() )

        ret = {'current':{}, 'info':{}, 'forecast':[], 'city': self.city, 'source': 'openweathermap'}
        ret['current']['temperature'] = int(resp['main']['temp'] + 0.5)
        ret['current']['weather_type'] = resp['weather'][0]['description']
        ret['current']['image'] = translate_icon(resp['weather'][0]['icon'])
        ret['current']['uptime'] = resp['dt']

        url = 'http://api.openweathermap.org/data/2.5/forecast?q=%s&lang=ru&units=metric' % urllib2.quote(self.cityid)
        resp = json.loads( urllib2.urlopen(url).read() )

        tempd = {}
        tempn = {}
        for cnt, it in enumerate(resp['list']):
            if 'night' not in ret['info'] and it['dt_txt'].endswith('03:00:00'):
                ret['info']['night'] = int(it['main']['temp'] + 0.5)
            if 'tomorrow' not in ret['info'] and it['dt_txt'].endswith('15:00:00') and cnt > 5:
                ret['info']['tomorrow'] = int(it['main']['temp'] + 0.5)
                
            if it['dt_txt'].endswith('15:00:00'):
                tempd[it['dt_txt'][:10]] = (int(it['main']['temp'] + 0.5), translate_icon(it['weather'][0]['icon']))
            if it['dt_txt'].endswith('03:00:00'):
                tempn[it['dt_txt'][:10]] = (int(it['main']['temp'] + 0.5), translate_icon(it['weather'][0]['icon'][:-1] + 'n'))

        foredays = set(tempd.keys()) & set(tempn.keys())
        for fd in sorted(foredays):
            d = {}
            d['date'] = fd
            d['day'] = {}
            d['day']['temperature'] = tempd[fd][0]
            d['day']['image'] = tempd[fd][1]
            d['night'] = {}
            d['night']['temperature'] = tempn[fd][0]
            d['night']['image'] = tempn[fd][1]
            ret['forecast'].append(d)

        return ret
