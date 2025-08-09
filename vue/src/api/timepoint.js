import request from '/@/utils/request'

// 获取模型列表
export function getModelNames() {
  return request({
    url: '/api/flask/model_names',
    method: 'get'
  })
}

// 时间点检测
export function predictTimePoint(data) {
  return request({
    url: '/api/flask/predictTimePoint',
    method: 'post',
    data
  })
}

// 获取时间点检测记录
export function getTimepointRecords(params) {
  return request({
    url: '/api/timepointRecords/page',
    method: 'get',
    params
  })
}

// 删除时间点检测记录
export function deleteTimepointRecord(id) {
  return request({
    url: `/api/timepointRecords/${id}`,
    method: 'delete'
  })
} 