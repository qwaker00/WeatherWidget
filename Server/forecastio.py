# coding=utf8
import urllib2, json
import datetime
from weather import Weather

def translate_icon(icon):
    trtable = {
        'clear-day': 'skc_d',
        'clear-night': 'skc_n',
        'snow': 'bl',
        'rain': 'ovc_+ra',
        'sleet': 'ovc_ra_sn',
        'wind': 'wnd',
        'fog': 'fg_d',
        'cloudy': 'ovc',
        'partly-cloudy-day': 'bkn_d',
        'partly-cloudy-night': 'bkn_n',
    }
    return trtable[icon]

class ForecastIOWeather(Weather):
    def __init__(self, city):
        url = 'http://maps.googleapis.com/maps/api/geocode/json?address=%s&sensor=false' % city.encode('utf8')
        loc = json.loads( urllib2.urlopen(url).read() )['results'][0]['geometry']['location']
        self.cityid = '%s,%s' % (loc['lat'], loc['lng'])
        self.city = city

    def get(self):
        url = 'https://api.forecast.io/forecast/8387f701f50fb586b03da1dc6d680f49/%s?units=si&exclude=minutely,daily,flags' % (self.cityid)
        resp = json.loads( urllib2.urlopen(url).read() )

        ret = {'current':{}, 'info':{}, 'forecast':[], 'city': self.city}
        ret['current']['temperature'] = int(resp['currently']['temperature'] + 0.5)
        ret['current']['weather_type'] = resp['currently']['summary']
        ret['current']['image'] = translate_icon(resp['currently']['icon'])
        ret['current']['uptime'] = resp['currently']['time']

        tempd = {}
        tempn = {}
        for cnt, h in enumerate(resp['hourly']['data']):
            if 'night' not in ret['info'] and h['time'] % 86400 == 10800:
                ret['info']['night'] = int(h['temperature'] + 0.5)
            if 'tomorrow' not in ret['info'] and h['time'] % 86400 == 54000 and cnt > 4:
                ret['info']['tomorrow'] = int(h['temperature'] + 0.5 + 0.5)


        day = resp['hourly']['data'][0]['time'] - resp['hourly']['data'][0]['time'] % 86400
        for it in xrange(1, 10):
            date = datetime.datetime.fromtimestamp(day + 86400 * it).strftime('%Y-%m-%d')
            url = 'https://api.forecast.io/forecast/8387f701f50fb586b03da1dc6d680f49/%s,%sT00:00:00?units=si&exclude=minutely,daily,flags' % (self.cityid, date)
            miniresp = json.loads( urllib2.urlopen(url).read() )            
            for cnt, h in enumerate(miniresp['hourly']['data']):
                date = datetime.datetime.fromtimestamp(h['time']).strftime('%Y-%m-%d')
                if h['time'] % 86400 == 10800:
                    tempn[date] = (int(h['temperature'] + 0.5), translate_icon(h['icon']))
                if h['time'] % 86400 == 54000:
                    tempd[date] = (int(h['temperature'] + 0.5 + 0.5), translate_icon(h['icon']))

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
