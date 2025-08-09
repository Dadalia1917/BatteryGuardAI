<template>
	<div class="layout-padding">
		<el-card class="box-card">
			<template #header>
				<div class="card-header">
					<span>电池单点检测记录</span>
					<div>
						<el-button type="primary" size="small" @click="getTableData" :icon="Refresh">刷新数据</el-button>
					</div>
				</div>
			</template>
			<div v-if="loading" class="loading-container">
				<el-skeleton :rows="5" animated />
			</div>
			<div v-else>
				<el-alert v-if="tableData.length === 0" type="info" show-icon :closable="false">
					暂无检测记录数据
				</el-alert>
				<el-table v-else :data="tableData" style="width: 100%">
				<el-table-column prop="username" label="用户名" align="center" />
					<el-table-column prop="batteryData" label="电池数据" align="center">
						<template #default="scope">
							<el-popover
								placement="right"
								trigger="hover"
								:width="400"
								popper-class="data-popover"
							>
								<template #default>
									<div class="data-content">
										<div v-if="typeof scope.row.batteryData === 'object'">
											<p v-for="(value, key) in scope.row.batteryData" :key="key" class="battery-data-item">
												<span class="data-key">{{ formatBatteryKey(key) }}：</span>
												<span class="data-value">{{ formatBatteryValue(value, key) }}</span>
											</p>
										</div>
										<pre v-else>{{ scope.row.batteryData }}</pre>
									</div>
								</template>
								<template #reference>
									<span class="hover-text">
										{{ typeof scope.row.batteryData === 'string' ? '电池数据' : '电池数据...' }}
									</span>
								</template>
							</el-popover>
						</template>
					</el-table-column>
				<el-table-column prop="anomalyScore" label="异常分数" align="center" />
				<el-table-column prop="isFault" label="是否故障" align="center">
					<template #default="scope">
						<el-tag :type="scope.row.isFault === 'true' ? 'danger' : 'success'">
							{{ scope.row.isFault === 'true' ? '异常' : '正常' }}
						</el-tag>
					</template>
				</el-table-column>
				<el-table-column prop="startTime" label="检测时间" align="center" />
					<el-table-column prop="ai" label="AI助手" align="center">
						<template #default="scope">
							<span>{{ formatAIDisplay(scope.row.ai) }}</span>
						</template>
					</el-table-column>
					<el-table-column prop="suggestion" label="AI建议" align="center">
						<template #default="scope">
							<el-popover
								placement="left"
								trigger="hover"
								:width="400"
								popper-class="suggestion-popover"
							>
								<template #default>
									<div class="suggestion-content" v-html="scope.row.suggestion ? scope.row.suggestion.replace(/\n/g, '<br/>') : '不使用AI'"></div>
								</template>
								<template #reference>
									<span class="hover-text">{{ scope.row.suggestion ? scope.row.suggestion.substring(0, 15) + '...' : '不使用AI' }}</span>
								</template>
							</el-popover>
						</template>
					</el-table-column>
				<el-table-column fixed="right" label="操作" width="180" align="center">
					<template #default="scope">
						<el-button type="primary" size="small" @click="openDetails(scope.row)">详情</el-button>
						<el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
					</template>
				</el-table-column>
			</el-table>
			<el-pagination
				@size-change="handleSizeChange"
				@current-change="handleCurrentChange"
				:current-page="currentPage"
				:page-sizes="[10, 20, 50, 100]"
				:page-size="pageSize"
				layout="total, sizes, prev, pager, next, jumper"
				:total="total"
				style="margin-top: 20px;"
			>
			</el-pagination>
			</div>
		</el-card>

		<!-- 详情对话框 -->
		<el-dialog v-model="detailsVisible" title="检测详情" width="70%">
			<template v-if="currentRecord">
				<el-descriptions :column="2" border>
					<el-descriptions-item label="用户名">{{ currentRecord.username }}</el-descriptions-item>
					<el-descriptions-item label="检测时间">{{ currentRecord.startTime }}</el-descriptions-item>
					<el-descriptions-item label="异常分数">{{ currentRecord.anomalyScore }}</el-descriptions-item>
					<el-descriptions-item label="是否故障">
						<el-tag :type="currentRecord.isFault === 'true' ? 'danger' : 'success'">
							{{ currentRecord.isFault === 'true' ? '异常' : '正常' }}
						</el-tag>
					</el-descriptions-item>
					<el-descriptions-item label="处理用时">{{ currentRecord.allTime }}秒</el-descriptions-item>
					<el-descriptions-item label="使用的AI">{{ formatAIDisplay(currentRecord.ai) }}</el-descriptions-item>
					<el-descriptions-item label="电池数据" :span="2">
						<div v-if="batteryDataObj" class="formatted-battery-data">
							<p v-for="(value, key) in batteryDataObj" :key="key" class="battery-data-item">
								<span class="data-key">{{ formatBatteryKey(key) }}：</span>
								<span class="data-value">{{ formatBatteryValue(value, key) }}</span>
							</p>
						</div>
						<pre v-else>{{ JSON.stringify(batteryDataObj, null, 2) }}</pre>
					</el-descriptions-item>
					<el-descriptions-item label="特征重要性" :span="2">
						<div id="featureImportanceChart" style="width: 100%; height: 300px;"></div>
					</el-descriptions-item>
					<el-descriptions-item label="AI建议" :span="2">
						<div v-html="formattedSuggestion || '不使用AI'"></div>
					</el-descriptions-item>
				</el-descriptions>
			</template>
		</el-dialog>
	</div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, markRaw } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '/@/utils/request';
import axios from 'axios';
import * as echarts from 'echarts';
import { getTimepointRecords, deleteTimepointRecord } from '/@/api/timepoint';
import { Refresh } from '@element-plus/icons-vue';

// 定义记录类型
interface TimepointRecord {
	id: number;
	username: string;
	batteryData: string;
	anomalyScore: string;
	featureImportance: string;
	allTime: string;
	conf: string;
	model: string;
	startTime: string;
	isFault: string;
	ai: string;
	suggestion: string;
}

const tableData = ref<TimepointRecord[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const detailsVisible = ref(false);
const currentRecord = ref<TimepointRecord | null>(null);
const batteryDataObj = ref<any>(null);
const formattedSuggestion = ref('');
const loading = ref(false);
let featureImportanceChart: echarts.ECharts | null = null;

const getTableData = async () => {
	try {
		loading.value = true;
		console.log('开始获取时间点检测记录...');
		console.log('请求参数:', { pageNum: currentPage.value, pageSize: pageSize.value });
		
		// 尝试直接使用axios请求，绕过request封装
		const directUrl = `/api/timepointRecords/page?pageNum=${currentPage.value}&pageSize=${pageSize.value}`;
		console.log('直接请求URL:', directUrl);
		
		try {
			// 默认清空数据，避免显示旧数据
			tableData.value = [];
			
			// 首先尝试直接调用后端API（不经过代理）
			try {
				const directBackendRes = await axios.get(`http://localhost:9999/timepointRecords/page?pageNum=${currentPage.value}&pageSize=${pageSize.value}`, {
					withCredentials: false
				});
				console.log('直接调用后端API响应:', directBackendRes);
				
				if (directBackendRes.status === 200 && directBackendRes.data) {
					processResponse(directBackendRes.data);
					loading.value = false;
					return;
				}
			} catch (directError) {
				console.error('直接调用后端API失败:', directError);
			}
			
			// 然后尝试使用标准API调用
		const res = await getTimepointRecords({
			pageNum: currentPage.value,
			pageSize: pageSize.value,
		});
			
			console.log('API调用响应:', JSON.stringify(res));
			processResponse(res);
		} catch (apiError) {
			console.error('API调用失败，尝试直接Axios请求:', apiError);
			
			// 尝试直接使用axios通过代理
			const axiosRes = await axios.get(directUrl, {
				withCredentials: false // 确保与后端CORS配置一致
			});
			console.log('直接Axios响应:', axiosRes);
			
			if (axiosRes.status === 200) {
				processResponse(axiosRes.data);
			} else {
				throw new Error(`Axios请求失败: ${axiosRes.status}`);
			}
		}
	} catch (error: any) {
		console.error('获取数据失败', error);
		// 提供更详细的错误信息
		if (error.response) {
			console.error('错误响应数据:', error.response.data);
			console.error('错误状态码:', error.response.status);
			ElMessage.error(`获取数据失败: 状态码 ${error.response.status}, 错误信息: ${error.message}`);
		} else if (error.request) {
			console.error('请求发送但未收到响应');
			ElMessage.error('获取数据失败: 服务器未响应');
		} else {
			console.error('请求配置错误:', error.message);
			ElMessage.error('获取数据失败: ' + error.message);
		}
		
		tableData.value = [];
		total.value = 0;
	} finally {
		loading.value = false;
	}
};

// 抽取响应处理逻辑为单独函数
const processResponse = (res) => {
	console.log('处理响应数据:', JSON.stringify(res).substring(0, 500));
	
	if (res.code === 0 || res.code === '0') {
		// 检查记录是否为空
		if (!res.data || !res.data.records || res.data.records.length === 0) {
			console.warn('获取到的记录为空');
			tableData.value = [];
			total.value = 0;
			return;
		}
		
		console.log('原始记录数据:', JSON.stringify(res.data.records).substring(0, 500));
			
			// 对数据进行处理，确保字段名称正确
			tableData.value = res.data.records.map(record => {
			// 尝试解析JSON字符串
			let parsedBatteryData = record.batteryData;
			if (typeof record.batteryData === 'string') {
				try {
					parsedBatteryData = JSON.parse(record.batteryData);
				} catch (e) {
					console.error('解析batteryData失败:', e);
				}
			}
			
				// 检查并规范化字段名称
				return {
					id: record.id,
					username: record.username,
				batteryData: parsedBatteryData || record.batteryData || record.battery_data,
					anomalyScore: record.anomalyScore || record.anomaly_score,
					featureImportance: record.featureImportance || record.feature_importance,
					allTime: record.allTime || record.all_time,
					conf: record.conf,
					model: record.model,
					startTime: record.startTime || record.start_time,
					isFault: record.isFault || record.is_fault,
					ai: record.ai,
					suggestion: record.suggestion
				};
			});
			
			total.value = res.data.total;
		console.log('处理后的记录数量:', tableData.value.length);
			console.log('记录总数:', res.data.total);
		} else {
			console.error('获取时间点检测记录失败:', res.msg);
		tableData.value = [];
		total.value = 0;
			ElMessage.error('获取数据失败: ' + res.msg);
	}
};

const handleSizeChange = (val) => {
	pageSize.value = val;
	getTableData();
};

const handleCurrentChange = (val) => {
	currentPage.value = val;
	getTableData();
};

const handleDelete = (row) => {
	ElMessageBox.confirm('确定要删除这条记录吗？', '提示', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning',
	})
		.then(async () => {
			try {
				// 添加加载状态
				loading.value = true;
				
				// 使用Axios直接请求，避免可能的代理问题
				const res = await axios.delete(`http://localhost:9999/timepointRecords/${row.id}`, {
					withCredentials: false
				});
				
				console.log('删除响应:', res);
				
				if (res.status === 200 && res.data && (res.data.code === 0 || res.data.code === '0')) {
					// 使用明确的格式
					ElMessage({
						message: '删除成功',
						type: 'success',
						showClose: true,
						duration: 2000
					});
					getTableData();
				} else {
					ElMessage.error(res.data?.msg || '删除失败');
				}
			} catch (error) {
				console.error('删除失败', error);
				
				// 尝试标准API删除方式
				try {
					const apiRes = await deleteTimepointRecord(row.id);
					if (apiRes.code === 0) {
						ElMessage({
							message: '删除成功',
							type: 'success',
							showClose: true,
							duration: 2000
						});
						getTableData();
						return;
					}
				} catch (apiError) {
					console.error('API删除失败:', apiError);
				}
				
				ElMessage.error('删除失败：' + ((error as Error).message || '未知错误'));
			} finally {
				loading.value = false;
			}
		})
		.catch(() => {
			// 取消删除操作
		});
};

const openDetails = (row) => {
	currentRecord.value = row;
	
	// 解析电池数据
	try {
		batteryDataObj.value = JSON.parse(row.batteryData);
	} catch (e) {
		batteryDataObj.value = row.batteryData;
	}
	
	// 格式化AI建议（将换行转为HTML）
	formattedSuggestion.value = row.suggestion ? row.suggestion.replace(/\n/g, '<br/>') : '不使用AI';
	
	detailsVisible.value = true;
	
	// 在下一个渲染周期绘制图表
	nextTick(() => {
		drawFeatureImportanceChart(row);
	});
};

const drawFeatureImportanceChart = (row: TimepointRecord) => {
	if (!row.featureImportance) return;
	
	try {
		let featureNames = ['总电压', '电流', '剩余电量', '最大单体电压', '最小单体电压', '最高温度', '最低温度'];
		let featureData: number[] = [];
		
		try {
			featureData = JSON.parse(row.featureImportance);
		} catch (e) {
			console.error('解析特征重要性数据失败', e);
			return;
		}
		
		// 如果已有图表实例，先销毁
		if (featureImportanceChart) {
			featureImportanceChart.dispose();
		}
		
		// 初始化图表
		const chartDom = document.getElementById('featureImportanceChart');
		if (!chartDom) {
			console.error('找不到图表DOM元素');
			return;
		}
		
		featureImportanceChart = markRaw(echarts.init(chartDom));
		
		// 格式化数据为图表需要的格式
		const formattedData = featureNames.map((name, index) => ({
			name,
			value: (featureData[index] * 100).toFixed(2)
		}));
		
		// 设置图表选项
		const option = {
			title: {
				text: '特征重要性分析',
				left: 'center'
			},
			tooltip: {
				trigger: 'item',
				formatter: '{b}: {c}%'
			},
			series: [
				{
					name: '特征重要性',
					type: 'pie',
					radius: '70%',
					center: ['50%', '60%'],
					data: formattedData,
					emphasis: {
						itemStyle: {
							shadowBlur: 10,
							shadowOffsetX: 0,
							shadowColor: 'rgba(0, 0, 0, 0.5)'
						}
					},
					label: {
						formatter: '{b}: {c}%'
					}
				}
			]
		};
		
		// 渲染图表
		if (featureImportanceChart) {
			featureImportanceChart.setOption(option);
		}
	} catch (error) {
		console.error('绘制特征重要性图表失败', error);
	}
};

const formatBatteryKey = (key: string | number): string => {
	// 将任何类型的键转换为字符串
	const keyStr = String(key);
	
	// 常见电池数据键名映射表
	const keyMap: Record<string, string> = {
		'volt': '电压',
		'current': '电流',
		'voltage': '电压',
		'temp': '温度',
		'temperature': '温度',
		'soc': '剩余电量',
		'capacity': '容量',
		'resistance': '内阻',
		'power': '功率',
		'max_cell_voltage': '最大单体电压',
		'min_cell_voltage': '最小单体电压',
		'max_temp': '最高温度',
		'min_temp': '最低温度',
		'cycle_count': '循环次数',
		'health': '健康状态',
		'status': '状态',
		// 添加其他可能的键名映射
		'maxCellVoltage': '最大单体电压',
		'minCellVoltage': '最小单体电压',
		'maxTemp': '最高温度',
		'minTemp': '最低温度'
	};
	
	return keyMap[keyStr] || keyStr;
};

const formatBatteryValue = (value: any, key: string | number): string => {
	// 将键转换为字符串以避免类型问题
	const keyStr = String(key);
	
	// 安全地将值转换为字符串
	const strValue = String(value);
	
	// 尝试解析为数字
	const numValue = Number(value);
	const isNumber = !isNaN(numValue);
	
	// 根据键名添加适当的单位
	if (isNumber) {
		if (keyStr.toLowerCase().includes('volt') || keyStr.toLowerCase().includes('voltage')) return `${strValue}V`;
		if (keyStr.toLowerCase().includes('current')) return `${strValue}A`;
		if (keyStr.toLowerCase().includes('temp')) return `${strValue}℃`;
		if (keyStr.toLowerCase().includes('soc')) return `${strValue}%`;
		if (keyStr.toLowerCase().includes('resistance')) return `${strValue}mΩ`;
		if (keyStr.toLowerCase().includes('power')) return `${strValue}W`;
		if (keyStr.toLowerCase().includes('capacity')) return `${strValue}mAh`;
	}
	
	return strValue;
};

const formatAIDisplay = (aiValue) => {
	if (!aiValue) return '不使用AI';
	
	// 处理LAN和Local后缀
	if (aiValue.endsWith('-LAN')) {
		return aiValue.replace('-LAN', '(局域网)');
	} else if (aiValue.endsWith('-Local')) {
		return aiValue.replace('-Local', '(本地)');
	}
	
	return aiValue;
};

onMounted(() => {
	getTableData();
});
</script>

<style scoped>
.box-card {
	margin-bottom: 20px;
}
.card-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
}
.loading-container {
	display: flex;
	justify-content: center;
	align-items: center;
	height: 200px;
}
.hover-text {
	cursor: pointer;
	color: #409eff;
	text-decoration: underline;
}
:deep(.data-popover), :deep(.suggestion-popover) {
	max-height: 400px;
	overflow-y: auto;
}
:deep(.data-content) pre {
	white-space: pre-wrap;
	word-break: break-word;
	margin: 0;
	font-family: monospace;
}
:deep(.suggestion-content) {
	white-space: pre-line;
	line-height: 1.5;
}
.battery-data-item {
	margin: 5px 0;
	line-height: 1.5;
}
.data-key {
	font-weight: bold;
	color: #606266;
}
.data-value {
	color: #303133;
}
.formatted-battery-data {
	padding: 10px;
	background-color: #f8f8f8;
	border-radius: 4px;
}
</style> 