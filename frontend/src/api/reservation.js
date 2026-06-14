import request from '@/utils/request'

export function createReservation(data) {
  return request({
    url: '/reservation/create',
    method: 'post',
    data
  })
}

export function cancelReservation(reservationId) {
  return request({
    url: `/reservation/cancel/${reservationId}`,
    method: 'post'
  })
}

export function getReservationList() {
  return request({
    url: '/reservation/list',
    method: 'get'
  })
}

export function getReservation(reservationId) {
  return request({
    url: `/reservation/${reservationId}`,
    method: 'get'
  })
}

export function getAvailablePorts(shedId, startTime, endTime) {
  return request({
    url: '/reservation/available-ports',
    method: 'get',
    params: { shedId, startTime, endTime }
  })
}

export function getQueuedReservations(shedId) {
  return request({
    url: '/reservation/queued',
    method: 'get',
    params: { shedId }
  })
}
