import Vue from 'vue'
import { getColumnConfig, saveColumnConfig, resetColumnConfig } from '@/api/api'

export const COLUMN_SETTING_FORCE_SYNC_EVENT = 'column-setting:force-sync'

function parseColumnSetting(configText) {
  if (!configText) return []
  if (Array.isArray(configText)) return configText.filter(Boolean)
  if (typeof configText === 'string') {
    if (configText.trim().charAt(0) === '[') {
      try {
        let parsed = JSON.parse(configText)
        return Array.isArray(parsed) ? parsed.filter(Boolean) : []
      } catch (e) {
        return []
      }
    }
    return configText.split(',').filter(Boolean)
  }
  return []
}

function normalizeColumnSetting(dataIndexArr, mergeSetting) {
  let arr = Array.isArray(dataIndexArr) ? dataIndexArr.filter(Boolean) : []
  return typeof mergeSetting === 'function' ? (mergeSetting(arr) || []) : arr
}

function readLocalColumnSetting(storageKey, mergeSetting) {
  if (!storageKey) return []
  return normalizeColumnSetting(parseColumnSetting(Vue.ls.get(storageKey)), mergeSetting)
}

function writeLocalColumnSetting(storageKey, dataIndexArr) {
  if (!storageKey) return
  Vue.ls.set(storageKey, (dataIndexArr || []).join(','))
}

function removeLocalColumnSetting(storageKey) {
  if (!storageKey) return
  Vue.ls.remove(storageKey)
}

export function bindColumnSettingForceSync(vm, handler) {
  if (vm && vm.$bus && typeof handler === 'function') {
    vm.$bus.$on(COLUMN_SETTING_FORCE_SYNC_EVENT, handler)
  }
}

export function unbindColumnSettingForceSync(vm, handler) {
  if (vm && vm.$bus && typeof handler === 'function') {
    vm.$bus.$off(COLUMN_SETTING_FORCE_SYNC_EVENT, handler)
  }
}

export function emitForceSyncColumnSettings(vm) {
  if (vm && vm.$bus) {
    vm.$bus.$emit(COLUMN_SETTING_FORCE_SYNC_EVENT, { at: Date.now() })
  }
}

export function loadColumnSetting({
  pageCode = '',
  storageKey = '',
  defaultDataIndex = [],
  applySetting,
  mergeSetting,
  syncLocalFallback = true,
  forceClearLocal = false
} = {}) {
  let defaultArr = normalizeColumnSetting(defaultDataIndex, mergeSetting)
  if (typeof applySetting === 'function') {
    applySetting(defaultArr)
  }
  if (forceClearLocal) {
    removeLocalColumnSetting(storageKey)
  }
  if (!pageCode) {
    let localArr = readLocalColumnSetting(storageKey, mergeSetting)
    if (localArr.length && typeof applySetting === 'function') {
      applySetting(localArr)
      return Promise.resolve(localArr)
    }
    return Promise.resolve(defaultArr)
  }
  return getColumnConfig({ pageCode }).then((res) => {
    if (res && res.code === 200 && res.data && res.data.columnConfig) {
      let serverArr = normalizeColumnSetting(parseColumnSetting(res.data.columnConfig), mergeSetting)
      if (serverArr.length) {
        if (typeof applySetting === 'function') {
          applySetting(serverArr)
        }
        writeLocalColumnSetting(storageKey, serverArr)
        return serverArr
      }
    }
    let localArr = readLocalColumnSetting(storageKey, mergeSetting)
    if (localArr.length) {
      if (typeof applySetting === 'function') {
        applySetting(localArr)
      }
      if (syncLocalFallback) {
        saveColumnSetting({ pageCode, storageKey, dataIndexArr: localArr, mergeSetting })
      }
      return localArr
    }
    return defaultArr
  }).catch(() => {
    let localArr = readLocalColumnSetting(storageKey, mergeSetting)
    if (localArr.length && typeof applySetting === 'function') {
      applySetting(localArr)
      return localArr
    }
    return defaultArr
  })
}

export function saveColumnSetting({
  pageCode = '',
  storageKey = '',
  dataIndexArr = [],
  mergeSetting
} = {}) {
  let finalArr = normalizeColumnSetting(dataIndexArr, mergeSetting)
  writeLocalColumnSetting(storageKey, finalArr)
  if (!pageCode) {
    return Promise.resolve(finalArr)
  }
  return saveColumnConfig({
    pageCode,
    columnConfig: JSON.stringify(finalArr)
  }).then(() => finalArr)
}

export function resetColumnSetting({
  pageCode = '',
  storageKey = '',
  defaultDataIndex = [],
  applySetting,
  mergeSetting
} = {}) {
  let defaultArr = normalizeColumnSetting(defaultDataIndex, mergeSetting)
  removeLocalColumnSetting(storageKey)
  if (typeof applySetting === 'function') {
    applySetting(defaultArr)
  }
  if (!pageCode) {
    return Promise.resolve(defaultArr)
  }
  return resetColumnConfig({ pageCode }).then(() => defaultArr).catch(() => defaultArr)
}

export function forceSyncColumnSetting(options = {}) {
  return loadColumnSetting({
    ...options,
    forceClearLocal: true,
    syncLocalFallback: false
  })
}
