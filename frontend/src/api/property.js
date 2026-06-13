import request from '@/utils/request'

export function getShedList() {
  return request({
    url: '/property/shed/list',
    method: 'get'
  })
}

export function createShed(data) {
  return request({
    url: '/property/shed',
    method: 'post',
    data
  })
}

export function updateShed(id, data) {
  return request({
    url: `/property/shed/${id}`,
    method: 'put',
    data
  })
}

export function deleteShed(id) {
  return request({
    url: `/property/shed/${id}`,
    method: 'delete'
  })
}

export function toggleShedStatus(id, status) {
  return request({
    url: `/property/shed/${id}/status`,
    method: 'put',
    params: { status }
  })
}

export function getPropertyShedList() {
  return request({
    url: '/property/shed/list',
    method: 'get'
  })
}

export function getPortList(shedId) {
  return request({
    url: '/property/port/list',
    method: 'get',
    params: { shedId }
  })
}

export function createPort(data) {
  return request({
    url: '/property/port',
    method: 'post',
    data
  })
}

export function updatePort(id, data) {
  return request({
    url: `/property/port/${id}`,
    method: 'put',
    data
  })
}

export function deletePort(id) {
  return request({
    url: `/property/port/${id}`,
    method: 'delete'
  })
}

export function togglePortStatus(id, status) {
  return request({
    url: `/property/port/${id}/status`,
    method: 'put',
    params: { status }
  })
}

export function getPropertyPortList(shedId) {
  return request({
    url: '/property/port/list',
    method: 'get',
    params: { shedId }
  })
}

export function getPricingRules(shedId) {
  return request({
    url: '/property/pricing/list',
    method: 'get',
    params: { shedId }
  })
}

export function createPricingRule(data) {
  return request({
    url: '/property/pricing',
    method: 'post',
    data
  })
}

export function updatePricingRule(id, data) {
  return request({
    url: `/property/pricing/${id}`,
    method: 'put',
    data
  })
}

export function deletePricingRule(id) {
  return request({
    url: `/property/pricing/${id}`,
    method: 'delete'
  })
}

export function getPropertyPricingList(shedId) {
  return request({
    url: '/property/pricing/list',
    method: 'get',
    params: { shedId }
  })
}

export function getAllBills(status) {
  return request({
    url: '/property/billing/list',
    method: 'get',
    params: { status }
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
