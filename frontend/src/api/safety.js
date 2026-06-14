import request from '@/utils/request'

export function reportTemperature(data) {
  return request({
    url: '/safety/temperature/report',
    method: 'post',
    data
  })
}

export function reportSmoke(data) {
  return request({
    url: '/safety/smoke/report',
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

export function resolveAlert(alertId, remark) {
  return request({
    url: `/safety/alert/resolve/${alertId}`,
    method: 'post',
    data: { remark }
  })
}

export function getAlertList(status, alertType) {
  return request({
    url: '/safety/alert/list',
    method: 'get',
    params: { status, alertType }
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

export function getPowerOffRecords(portId) {
  return request({
    url: `/safety/power-off-records/${portId}`,
    method: 'get'
  })
}

export function reviewUnfreeze(data) {
  return request({
    url: '/safety/review/unfreeze',
    method: 'post',
    data
  })
}

export function reviewResume(data) {
  return request({
    url: '/safety/review/resume',
    method: 'post',
    data
  })
}

export function getReviewRecords(vehicleId) {
  return request({
    url: `/safety/review-records/${vehicleId}`,
    method: 'get'
  })
}

export function getFrozenVehicles() {
  return request({
    url: '/safety/frozen-vehicles',
    method: 'get'
  })
}
