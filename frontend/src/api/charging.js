import request from '@/utils/request'

export function startCharging(data) {
  return request({
    url: '/charging/start',
    method: 'post',
    data
  })
}

export function stopCharging(data) {
  return request({
    url: '/charging/stop',
    method: 'post',
    data
  })
}

export function getCurrentCharging() {
  return request({
    url: '/charging/current',
    method: 'get'
  })
}

export function getChargingRecords() {
  return request({
    url: '/charging/records',
    method: 'get'
  })
}

export function getChargingRecord(recordId) {
  return request({
    url: `/charging/record/${recordId}`,
    method: 'get'
  })
}

export function getBills() {
  return request({
    url: '/charging/bills',
    method: 'get'
  })
}

export function getUnpaidBills() {
  return request({
    url: '/charging/bills/unpaid',
    method: 'get'
  })
}

export function payBill(billId, paymentMethod, amount) {
  return request({
    url: '/charging/pay',
    method: 'post',
    data: { billId, paymentMethod, amount }
  })
}
