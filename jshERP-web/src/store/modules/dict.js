const state = {
  dict: []
}
const mutations = {
  SET_DICT: (state, { key, value }) => {
    if (key === null || key === '') return
    // 已存在则更新，避免重复 push 导致无限增长
    const idx = state.dict.findIndex(d => d.key === key)
    if (idx >= 0) {
      state.dict.splice(idx, 1, { key, value })
    } else {
      state.dict.push({ key, value })
    }
  },
  REMOVE_DICT: (state, key) => {
    try {
      const idx = state.dict.findIndex(d => d.key === key)
      if (idx >= 0) {
        state.dict.splice(idx, 1)  // 修复：原为 splice(i, i)，删除 0 个元素
      }
    } catch (e) { /* ignore */ }
  },
  CLEAN_DICT: (state) => {
    state.dict = []
  }
}

const actions = {
  // 设置字典
  setDict({ commit }, data) {
    commit('SET_DICT', data)
  },
  // 删除字典
  removeDict({ commit }, key) {
    commit('REMOVE_DICT', key)
  },
  // 清空字典
  cleanDict({ commit }) {
    commit('CLEAN_DICT')
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}

