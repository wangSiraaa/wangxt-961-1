import { defineStore } from 'pinia'
import { login, getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),
  getters: {
    isLoggedIn: state => !!state.token,
    userRole: state => state.userInfo?.role || '',
    isResident: state => state.userInfo?.role === 'RESIDENT',
    isProperty: state => state.userInfo?.role === 'PROPERTY',
    isSafetyOfficer: state => state.userInfo?.role === 'SAFETY_OFFICER'
  },
  actions: {
    async login(loginData) {
      const data = await login(loginData)
      this.token = data.token
      this.userInfo = {
        userId: data.userId,
        username: data.username,
        role: data.role,
        isVerified: data.isVerified,
        balance: data.balance,
        realName: data.realName
      }
      localStorage.setItem('token', data.token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      return data
    },
    async fetchUserInfo() {
      const data = await getUserInfo()
      this.userInfo = {
        ...this.userInfo,
        ...data
      }
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      return data
    },
    updateBalance(balance) {
      if (this.userInfo) {
        this.userInfo.balance = balance
        localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      }
    },
    updateVerified(isVerified) {
      if (this.userInfo) {
        this.userInfo.isVerified = isVerified
        localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      }
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
