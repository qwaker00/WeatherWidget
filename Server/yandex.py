# coding=utf8
import urllib2, json
import xml.etree.ElementTree as ET
from weather import Weather
import time

class YandexWeather(Weather):
    def __init__(self, city):
        citydata = ET.fromstring(urllib2.urlopen('http://weather.yandex.ru/static/cities.xml').read())
        for c in citydata.findall('country/city'):
            if c.text == city:
                self.cityid = c.attrib['id']
                self.city = city
                return        
        raise "City not found"
    
    namespaces = {'fc': 'http://weather.yandex.ru/forecast'}

    def get(self):
        url = 'http://export.yandex.ru/weather-ng/forecasts/%s.xml' % (self.cityid)
        response = ET.fromstring(urllib2.urlopen(url).read())

        ret = {'current':{}, 'info':{}, 'forecast':[], 'city': self.city}

        now_temp = response.find('fc:fact/fc:temperature', namespaces=self.namespaces)
        if now_temp is not None:
            ret['current']['temperature'] = int(now_temp.text)

        now_wt = response.find('fc:fact/fc:weather_type', namespaces=self.namespaces)
        if now_wt is not None:
            ret['current']['weather_type'] = now_wt.text

        now_im = response.find('fc:fact/fc:image-v3', namespaces=self.namespaces)
        if now_im is not None:
            ret['current']['image'] = now_im.text

        now_time = response.find('fc:fact/fc:observation_time', namespaces=self.namespaces)
        if now_time is not None:
            ret['current']['uptime'] = int(time.mktime(time.strptime(now_time.text, '%Y-%m-%dT%H:%M:%S')))

        tomorrow_temp = response.find('fc:informer/fc:temperature[@type="tomorrow"]', namespaces=self.namespaces)
        if tomorrow_temp is not None:
            ret['info']['tomorrow'] = int(tomorrow_temp.text)

        night_temp = response.find('fc:informer/fc:temperature[@type="night"]', namespaces=self.namespaces)
        if night_temp is not None:
            ret['info']['night'] = int(night_temp.text)

        for day in response.findall('fc:day', namespaces=self.namespaces):
            d = {}
            d['date'] = day.get('date', '')
            
            day_part = day.find('fc:day_part[@type="day_short"]', namespaces=self.namespaces)
            if day_part is not None:
                d['day'] = {}
                d['day']['temperature'] = int(day_part.find('fc:temperature', namespaces=self.namespaces).text)
                d['day']['image'] = day_part.find('fc:image-v3', namespaces=self.namespaces).text

            night_part = day.find('fc:day_part[@type="night_short"]', namespaces=self.namespaces)
            if night_part is not None:
                d['night'] = {}
                d['night']['temperature'] = int(night_part.find('fc:temperature', namespaces=self.namespaces).text)
                d['night']['image'] = night_part.find('fc:image-v3', namespaces=self.namespaces).text

            ret['forecast'].append(d)
        return ret
