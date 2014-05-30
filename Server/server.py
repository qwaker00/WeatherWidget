# coding=utf8
import BaseHTTPServer
import SocketServer
import json
import gzip
import urllib2

PORT = 8181

import weather
import yandex
import openweathermap
import forecastio

class Aggregator(weather.Weather):
    def __init__(self, city):
        self.sources = (
                    yandex.YandexWeather(city),
                    openweathermap.OWMWeather(city),
                    forecastio.ForecastIOWeather(city),
                  )        
    def get(self):
        count = len(self.sources)
        response = [s.get() for s in self.sources]

        base = response[0]
        base['current']['temperature'] = (sum([r['current']['temperature'] for r in response]) + count/2) / count

        base['info']['tomorrow'] = (sum([r['info']['tomorrow'] for r in response]) + count/2) / count
        base['info']['night'] = (sum([r['info']['night'] for r in response]) + count/2) / count

        dd = {}
        for r in reversed(response):
            for d in r['forecast']:
                if d['date'] not in dd:
                    dd[ d['date'] ] = d
                else:
                    dfc = dd[ d['date'] ]
                    dfc['day']['temperature'] = (dfc['day']['temperature'] + d['day']['temperature'] + 1) / 2
                    dfc['night']['temperature'] = (dfc['night']['temperature'] + d['night']['temperature'] + 1) / 2
                    
        base['forecast'] = []
        for d in sorted(dd.keys()):
            base['forecast'].append(dd[d])

        return base


city_cache = {}
def get_city_by_ip(ip):
    if ip not in city_cache:
        city = json.loads( urllib2.urlopen('http://api.db-ip.com/addrinfo?addr=%s&api_key=a37152e59858fba80a04d6e33e4f144eb459b452' % ip).read() )['city']
        city_cache[ip] = json.loads( urllib2.urlopen('http://maps.googleapis.com/maps/api/geocode/json?address=%s&language=ru&sensor=false' % city).read() )['results'][0]['address_components'][0]['short_name']
    return city_cache[ip]
    

class WeatherHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    weathers = {}

    def get_weather_forcity(self, city):
        if city not in self.weathers:
            self.weathers[city] = Aggregator(city)
        return self.weathers[city].get_with_cache()

    def do_GET(self):
        self.send_response(200)
        self.send_header("Content-type", "application/json")
        self.send_header("Content-encoding", "gzip")
        self.end_headers()

        params = {}
        if self.path.find('?') != -1:
            for p in self.path[self.path.find('?') + 1:].split('&'):
                params[p[:p.find('=')]] = p[p.find('=') + 1:]

        if 'city' in params:
            city = urllib2.unquote(params['city']).decode('utf8')
        else:
            city = get_city_by_ip(self.client_address[0])

        with gzip.GzipFile(fileobj=self.wfile, mode="w") as output:            
            answer = json.dumps(self.get_weather_forcity(city), separators=(',', ':'))
            output.write(answer)

httpd = SocketServer.TCPServer(('', PORT), WeatherHandler)
httpd.serve_forever()
