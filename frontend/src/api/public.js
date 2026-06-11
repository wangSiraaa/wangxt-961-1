import request from '@/utils/request'

export function getShedList() {
  return request({
    url: '/public/shed/list',
    method: 'get'
  })
}

export function getShed(id) {
  return request({
    url: `/public/shed/${id}`,
    method: 'get'
  })
}

export function getPortList(shedId) {
  return request({
    url: '/public/port/list',
    method: 'get',
    params: { shedId }
  })
}

export function getPort(id) {
  return request({
    url: `/public/port/${id}`,
    method: 'get'
  })
}

export function getPricingRule(shedId) {
  return request({
    url: `/public/pricing/${shedId}`,
    method: 'get'
  })
}

export function getShedStatus(shedId) {
  return request({
    url: '/public/shed/status',
    method: 'get',
    params: { shedId }
  })
}

export function getAvailablePorts(shedId, startTime, endTime) {
  return request({
    url: '/public/port/available',
    method: 'get',
    params: { shedId, startTime, endTime }
  })
}
