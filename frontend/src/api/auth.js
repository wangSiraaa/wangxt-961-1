import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function getUserInfo() {
  return request({
    url: '/auth/info',
    method: 'get'
  })
}

export function verifyIdentity(data) {
  return request({
    url: '/user/verify',
    method: 'post',
    data
  })
}

export function recharge(amount) {
  return request({
    url: '/user/recharge',
    method: 'post',
    params: { amount }
  })
}

export function getBalance() {
  return request({
    url: '/user/balance',
    method: 'get'
  })
}
