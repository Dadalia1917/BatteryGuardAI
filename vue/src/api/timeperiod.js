import request from '/@/utils/request'

// 时间段检测
export function predictTimePeriod(data) {
  return request({
    url: '/api/flask/predictTimePeriod',
    method: 'post',
    data,
    // 因为是文件上传，需要设置特殊的Content-Type
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取时间段检测记录
export function getTimeperiodRecords(params) {
  return request({
    url: '/api/timeperiodRecords/page',
    method: 'get',
    params
  })
}

// 删除时间段检测记录
export function deleteTimeperiodRecord(id) {
  return request({
    url: `/api/timeperiodRecords/${id}`,
    method: 'delete'
  })
} 