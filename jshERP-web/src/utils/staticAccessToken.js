import Cookies from 'js-cookie'

export const STATIC_ACCESS_TOKEN_COOKIE = 'Static-Access-Token'

function getStaticAccessCookiePath() {
  const baseUrl = window._CONFIG['domianURL'] || '/jshERP-boot'
  try {
    const pathname = new URL(baseUrl, window.location.origin).pathname.replace(/\/$/, '')
    return pathname + '/systemConfig/static'
  } catch (e) {
    return '/jshERP-boot/systemConfig/static'
  }
}

export function syncStaticAccessToken(token) {
  const path = getStaticAccessCookiePath()
  if (token) {
    Cookies.set(STATIC_ACCESS_TOKEN_COOKIE, token, {
      expires: 7,
      path,
      sameSite: 'Lax',
      secure: window.location.protocol === 'https:'
    })
  } else {
    Cookies.remove(STATIC_ACCESS_TOKEN_COOKIE, { path })
  }
}

export function clearStaticAccessToken() {
  syncStaticAccessToken('')
}
