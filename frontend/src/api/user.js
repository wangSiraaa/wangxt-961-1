import request from '@/utils/request'

export function verifyUser(data) {
  return request({
    url: '/user/verify',
    method: 'post',
    data
  })
}

export function getBalance() {
  return request({
    url: '/user/balance',
    method: 'get'
  })
}

export function recharge(amount) {
  return request({
    url: '/user/recharge',
    method: 'post',
    params: { amount }
  })
}
