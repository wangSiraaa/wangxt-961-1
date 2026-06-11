import request from '@/utils/request'

export function reportTemperature(data) {
  return request({
    url: '/safety/temperature/report',
    method: 'post',
    data
  })
}

export function handleAlert(alertId, data) {
  return request({
    url: `/safety/alert/handle/${alertId}`,
    method: 'post',
    data
  })
}

export function getAlertList(status, level) {
  return request({
    url: '/safety/alert/list',
    method: 'get',
    params: { status, level }
  })
}

export function getAlert(alertId) {
  return request({
    url: `/safety/alert/${alertId}`,
    method: 'get'
  })
}

export function powerOff(portId) {
  return request({
    url: `/safety/power/off/${portId}`,
    method: 'post'
  })
}

export function powerOn(portId) {
  return request({
    url: `/safety/power/on/${portId}`,
    method: 'post'
  })
}

export function getHighTemperatureAlerts() {
  return request({
    url: '/safety/high-temperature',
    method: 'get'
  })
}
