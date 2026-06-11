import request from '@/utils/request'

export function bindVehicle(data) {
  return request({
    url: '/vehicle/bind',
    method: 'post',
    data
  })
}

export function verifyVehicle(vehicleId) {
  return request({
    url: `/vehicle/verify/${vehicleId}`,
    method: 'post'
  })
}

export function unbindVehicle(vehicleId) {
  return request({
    url: `/vehicle/unbind/${vehicleId}`,
    method: 'delete'
  })
}

export function getVehicleList() {
  return request({
    url: '/vehicle/list',
    method: 'get'
  })
}

export function getVehicle(vehicleId) {
  return request({
    url: `/vehicle/${vehicleId}`,
    method: 'get'
  })
}
