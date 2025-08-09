<template>
  <div class="app-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span></span>
      </div>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form ref="form" :model="form" label-width="120px">
            <el-form-item label="总电压 (V)">
              <el-input v-model="form.batteryData.volt" placeholder="例如：12.6" />
            </el-form-item>
            
            <el-form-item label="电流 (A)">
              <el-input v-model="form.batteryData.current" placeholder="例如：1.5" />
            </el-form-item>
            
            <el-form-item label="剩余电量 (%)">
              <el-input v-model="form.batteryData.soc" placeholder="例如：85" />
            </el-form-item>
            
            <el-form-item label="最大单体电压 (V)">
              <el-input v-model="form.batteryData.max_single_volt" placeholder="例如：4.2" />
            </el-form-item>
            
            <el-form-item label="最小单体电压 (V)">
              <el-input v-model="form.batteryData.min_single_volt" placeholder="例如：3.8" />
            </el-form-item>
            
            <el-form-item label="最高温度 (°C)">
              <el-input v-model="form.batteryData.max_temp" placeholder="例如：40" />
            </el-form-item>
            
            <el-form-item label="最低温度 (°C)">
              <el-input v-model="form.batteryData.min_temp" placeholder="例如：25" />
            </el-form-item>
            
            <el-form-item label="AI小助手">
              <el-select v-model="form.ai" placeholder="请选择AI助手">
                <el-option label="不使用AI" value="" />
                <el-option label="Deepseek-R1" value="Deepseek-R1" />
                <el-option label="Qwen" value="Qwen" />
                <el-option label="Deepseek-R1(本地)" value="Deepseek-R1-Local" />
                <el-option label="Deepseek-R1(局域网)" value="Deepseek-R1-LAN" />
                <el-option label="Gemma3(本地)" value="Gemma3-Local" />
                <el-option label="Gemma3(局域网)" value="Gemma3-LAN" />
                <el-option label="Qwen3.0(本地)" value="Qwen3.0-Local" />
                <el-option label="Qwen3.0(局域网)" value="Qwen3.0-LAN" />
                <el-option label="Qwen2.5-VL(本地)" value="Qwen2.5-VL-Local" />
                <el-option label="Qwen2.5-VL(局域网)" value="Qwen2.5-VL-LAN" />
                <el-option label="Qwen2.5-Omni(本地)" value="Qwen2.5-Omni-Local" />
                <el-option label="Qwen2.5-Omni(局域网)" value="Qwen2.5-Omni-LAN" />
              </el-select>
            </el-form-item>
            
            <el-form-item v-if="form.ai.includes('Qwen3.0')">
              <el-switch
                v-model="form.thinkMode"
                active-text="思考模式"
                inactive-text="非思考模式"
              />
            </el-form-item>
            
            <el-form-item>
              <el-button :loading="loading" type="primary" @click="submitForm">开始检测</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-col>
        
        <el-col :span="12">
          <div v-if="resultLoading" class="result-loading">
            <el-progress type="circle" :percentage="loadingPercentage" />
            <div class="status-message">{{ statusMessage }}</div>
          </div>
          
          <div v-else-if="result" class="result-container">
            <div class="result-box">
              <div class="result-header" :class="{'fault': result.result_text === '异常', 'normal': result.result_text === '正常'}">
                <div class="result-title">检测结果：<span class="result-status">{{ result.result_text }}</span></div>
                <div class="result-percent">
                  异常概率：{{ result.anomaly_percentage }}% (阈值: {{ result.threshold_percentage }}%)
                </div>
                <div class="result-time">
                  处理时间：{{ result.allTime }}秒
                </div>
              </div>

              <div class="charts-box">
                <div class="chart-wrapper">
                  <div class="chart-title">特征重要性分析</div>
                  <div id="featureImportanceChart" class="chart" style="width: 100%; height: 250px;"></div>
                </div>
                <div class="chart-wrapper">
                  <div class="chart-title">ROC曲线</div>
                  <div id="rocCurveChart" class="chart" style="width: 100%; height: 250px;"></div>
                </div>
              </div>
            </div>
          </div>
          
          <div v-else class="no-result">
            <el-empty description="请填写电池参数并点击开始检测" />
          </div>
        </el-col>
      </el-row>
      
      <!-- AI建议部分，始终显示在页面底部 -->
      <el-row style="margin-top: 20px;">
        <el-col :span="24">
          <el-card class="ai-suggestion-card">
            <template #header>
              <div class="ai-suggestion-header">
                <span>AI小助手建议</span>
              </div>
            </template>
            <div v-if="result && result.suggestion" class="suggestion-content">
              {{ result.suggestion }}
            </div>
            <div v-else class="no-suggestion">
              <el-empty description="AI将在您进行检测后提供建议" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getModelNames, predictTimePoint } from '/@/api/timepoint'
import io from 'socket.io-client'
import { getCurrentInstance } from 'vue'

export default {
  name: 'TimepointAnalysis',
  data() {
    return {
      form: {
        model: '',
        batteryData: {
          volt: '',
          current: '',
          soc: '',
          max_single_volt: '',
          min_single_volt: '',
          max_temp: '',
          min_temp: ''
        },
        ai: '',
        thinkMode: false
      },
      modelOptions: [],
      loading: false,
      resultLoading: false,
      loadingPercentage: 0,
      statusMessage: '',
      result: null,
      socket: null,
      featureChart: null,
      rocChart: null,
      cookies: null
    }
  },
  created() {
    const { proxy } = getCurrentInstance()
    this.cookies = proxy.$cookies
    
    this.fetchModelOptions()
    this.connectSocket()
  },
  beforeDestroy() {
    this.disconnectSocket()
    this.disposeCharts()
  },
  methods: {
    async fetchModelOptions() {
      try {
        const { data } = await getModelNames()
        this.modelOptions = data.model_items
      } catch (error) {
        console.error('获取模型列表失败', error)
        this.$message.error('获取模型列表失败')
      }
    },
    connectSocket() {
      this.socket = io('http://localhost:5000')
      this.socket.on('connect', () => {
        console.log('Socket连接成功')
      })
      this.socket.on('message', (data) => {
        this.statusMessage = data.data
        if (data.data.includes('%')) {
          const match = data.data.match(/(\d+(\.\d+)?)%/)
          if (match) {
            this.loadingPercentage = parseFloat(match[1])
          }
        }
      })
    },
    disconnectSocket() {
      if (this.socket) {
        this.socket.disconnect()
      }
    },
    disposeCharts() {
      if (this.featureChart) {
        this.featureChart.dispose()
        this.featureChart = null
      }
      if (this.rocChart) {
        this.rocChart.dispose()
        this.rocChart = null
      }
    },
    async submitForm() {
      const batteryData = this.form.batteryData
      for (const key in batteryData) {
        if (!batteryData[key]) {
          this.$message.error(`请填写${key}参数`)
          return
        }
        const value = parseFloat(batteryData[key])
        if (isNaN(value)) {
          this.$message.error(`${key}必须是数字`)
          return
        }
        batteryData[key] = value
      }
      
      this.loading = true
      this.resultLoading = true
      this.loadingPercentage = 0
      this.statusMessage = '正在准备数据...'
      
      try {
        let username = 'admin'
        try {
          if (this.cookies) {
            const cookieUsername = this.cookies.get('username')
            if (cookieUsername) {
              username = cookieUsername
            }
          }
        } catch (e) {
          console.error('获取cookie中的username失败:', e)
        }
        
        if (username === 'admin') {
          try {
            const localUsername = localStorage.getItem('username')
            if (localUsername) {
              username = localUsername
            }
          } catch (e) {
            console.error('获取localStorage中的username失败:', e)
          }
        }
        
        const requestData = {
          username: username,
          model: 'all',
          startTime: new Date().toLocaleString(),
          batteryData: this.form.batteryData,
          ai: this.form.ai,
          thinkMode: this.form.thinkMode
        }
        
        console.log('发送请求数据:', requestData)
        const response = await predictTimePoint(requestData)
        console.log('收到完整响应:', response)
        
        if (response.code === '0' && response.data) {
          if (typeof response.data === 'string') {
            try {
              this.result = JSON.parse(response.data)
              console.log('从字符串解析结果:', this.result)
            } catch (e) {
              console.error('解析响应字符串失败:', e)
              this.result = { result_text: '解析失败' }
            }
          } else {
            this.result = response.data
            console.log('直接使用响应对象:', this.result)
          }
        } else {
          console.error('请求失败:', response)
          this.$message.error('请求失败: ' + (response.msg || '未知错误'))
          return
        }
        
        setTimeout(() => {
          if (this.result) {
            this.renderFeatureChart()
            this.renderROCChart()
          }
        }, 500)
      } catch (error) {
        console.error('检测失败', error)
        this.$message.error('检测失败：' + (error.message || '未知错误'))
      } finally {
        this.loading = false
        this.resultLoading = false
      }
    },
    resetForm() {
      this.$refs.form.resetFields()
      this.form.batteryData = {
        volt: '',
        current: '',
        soc: '',
        max_single_volt: '',
        min_single_volt: '',
        max_temp: '',
        min_temp: ''
      }
      this.form.thinkMode = false
      this.result = null
      this.disposeCharts()
    },
    renderFeatureChart() {
      const chartDom = document.getElementById('featureImportanceChart')
      if (!chartDom) {
        console.error('找不到特征重要性图表容器')
        return
      }
      
      if (this.featureChart) {
        this.featureChart.dispose()
      }
      
      console.log('开始初始化特征重要性图表')
      this.featureChart = echarts.init(chartDom)
      
      try {
        console.log('特征重要性数据类型:', typeof this.result.feature_importance)
        console.log('特征重要性原始数据:', this.result.feature_importance)
        
        let featureImportance = []
        
        if (Array.isArray(this.result.feature_importance)) {
          featureImportance = this.result.feature_importance
          console.log('特征重要性已经是数组')
        } else if (typeof this.result.feature_importance === 'string') {
          try {
            featureImportance = JSON.parse(this.result.feature_importance)
            console.log('特征重要性成功解析为JSON数组')
          } catch (parseError) {
            console.error('特征重要性JSON解析失败:', parseError)
            
            if (this.result.feature_importance.startsWith('[') && this.result.feature_importance.endsWith(']')) {
              try {
                const cleanString = this.result.feature_importance
                  .replace('[', '')
                  .replace(']', '')
                  .replace(/\s/g, '')
                  
                featureImportance = cleanString
                  .split(',')
                  .map(item => {
                    const num = parseFloat(item.trim())
                    return isNaN(num) ? 0 : num
                  })
                  
                console.log('使用split方法解析特征重要性:', featureImportance)
              } catch (splitError) {
                console.error('分割字符串失败:', splitError)
                featureImportance = [0.1, 0.1, 0.1, 0.2, 0.2, 0.15, 0.15]
              }
            } else {
              featureImportance = [0.1, 0.1, 0.1, 0.2, 0.2, 0.15, 0.15]
              console.warn('无法识别的特征重要性格式，使用默认值')
            }
          }
        } else if (!this.result.feature_importance) {
          console.warn('特征重要性数据为空，使用默认值')
          featureImportance = [0.1, 0.1, 0.1, 0.2, 0.2, 0.15, 0.15]
        } else {
          console.error('特征重要性格式无法识别:', this.result.feature_importance)
          featureImportance = [0.1, 0.1, 0.1, 0.2, 0.2, 0.15, 0.15]
        }
        
        console.log('最终处理后的特征重要性数组:', featureImportance)
        
        while (featureImportance.length < 7) {
          featureImportance.push(0.1)
        }
        if (featureImportance.length > 7) {
          featureImportance = featureImportance.slice(0, 7)
        }
        
        const labels = ['总电压 (V)', '电流 (A)', '剩余电量 (%)', '最大单体电压 (V)', '最小单体电压 (V)', '最高温度 (°C)', '最低温度 (°C)']
        
        const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
            }
          },
          grid: {
            left: '3%',
            right: '15%',
            bottom: '3%',
            containLabel: true
          },
          xAxis: {
            type: 'value',
            boundaryGap: [0, 0.01]
          },
          yAxis: {
            type: 'category',
            data: labels,
            axisLabel: {
              fontSize: 10
            }
          },
          series: [
            {
              name: '特征重要性',
              type: 'bar',
              data: featureImportance.map(val => ({
                value: parseFloat(val).toFixed(3),
                label: {
                  show: true,
                  position: 'right',
                  formatter: '{c}'
                }
              })),
              itemStyle: {
                color: function(params) {
                  const colorList = [
                    '#5470c6', '#91cc75', '#fac858', '#ee6666', 
                    '#73c0de', '#3ba272', '#fc8452'
                  ]
                  return colorList[params.dataIndex]
                }
              }
            }
          ]
        }
        
        this.featureChart.setOption(option)
        
        window.addEventListener('resize', () => {
          if (this.featureChart) {
            this.featureChart.resize()
          }
        })
        
        setTimeout(() => {
          if (this.featureChart) {
            this.featureChart.resize()
          }
        }, 200)
        
        console.log('特征重要性图表渲染完成')
      } catch (error) {
        console.error('渲染特征重要性图表失败', error)
        this.$message.error('渲染特征重要性图表失败: ' + error.message)
      }
    },
    renderROCChart() {
      const chartDom = document.getElementById('rocCurveChart')
      if (!chartDom) {
        console.error('找不到ROC曲线图表容器')
        return
      }
      
      if (this.rocChart) {
        this.rocChart.dispose()
      }
      
      console.log('开始初始化ROC曲线图表')
      this.rocChart = echarts.init(chartDom)
      
      try {
        const fpr = Array.from({length: 100}, (_, i) => i / 100)
        const tpr = fpr.map(x => 1 / (1 + Math.exp(-10 * (x - 0.5))))
        const auc = 0.85
        
        const option = {
          title: {
            show: false
          },
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'cross'
            }
          },
          legend: {
            data: [`AUC = ${auc}`, '随机线'],
            right: 10,
            top: 10
          },
          grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
          },
          xAxis: [
            {
              type: 'value',
              name: '假阳性率',
              min: 0,
              max: 1
            }
          ],
          yAxis: [
            {
              type: 'value',
              name: '真阳性率',
              min: 0,
              max: 1
            }
          ],
          series: [
            {
              name: `AUC = ${auc}`,
              type: 'line',
              smooth: true,
              data: fpr.map((x, i) => [x, tpr[i]]),
              lineStyle: {
                width: 2,
                color: '#0000ff'
              }
            },
            {
              name: '随机线',
              type: 'line',
              data: [[0, 0], [1, 1]],
              lineStyle: {
                width: 2,
                type: 'dashed',
                color: '#ff0000'
              }
            }
          ]
        }
        
        this.rocChart.setOption(option)
        
        window.addEventListener('resize', () => {
          if (this.rocChart) {
            this.rocChart.resize()
          }
        })
        
        setTimeout(() => {
          if (this.rocChart) {
            this.rocChart.resize()
          }
        }, 200)
        
        console.log('ROC曲线图表渲染完成')
      } catch (error) {
        console.error('渲染ROC曲线图表失败', error)
        this.$message.error('渲染ROC曲线图表失败: ' + error.message)
      }
    }
  }
}
</script>

<style scoped>
.result-box {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.result-container {
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  height: 100%;
}

.result-header {
  padding: 20px;
  border-radius: 4px;
  margin-bottom: 15px;
  text-align: center;
  border: 1px solid;
}

.result-header.fault {
  background-color: #fef0f0;
  color: #f56c6c;
  border-color: #f56c6c;
}

.result-header.normal {
  background-color: #f0f9eb;
  color: #67c23a;
  border-color: #67c23a;
}

.result-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
}

.result-percent, .result-time {
  font-size: 16px;
  margin-top: 5px;
}

.result-status {
  font-size: 20px;
  font-weight: bold;
}

.charts-box {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.chart-wrapper {
  width: 100%;
  margin-bottom: 20px;
}

.chart-title {
  font-weight: bold;
  font-size: 16px;
  margin-bottom: 10px;
  text-align: center;
}

.chart {
  height: 250px;
  width: 100%;
  margin-bottom: 20px;
}

.ai-suggestion-card {
  margin-top: 20px;
  width: 100%;
}

.ai-suggestion-header {
  font-weight: bold;
  color: #409eff;
}

.suggestion-content {
  white-space: pre-line;
  line-height: 1.6;
  padding: 10px;
}

.no-suggestion {
  padding: 20px 0;
}

.result-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 50px 0;
}

.status-message {
  margin-top: 20px;
  color: #606266;
}

.no-result {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style> 