<template>
	<div class="home-container layout-pd">
		<el-row :gutter="15" class="home-card-two mb15">
			<el-col :xs="24" :sm="14" :md="14" :lg="16" :xl="16">
				<div class="home-card-item">
					<div style="height: 100%" ref="homeBarRef"></div>
				</div>
			</el-col>
			<el-col :xs="24" :sm="10" :md="10" :lg="8" :xl="8" class="home-media">
				<div class="home-card-item">
					<div style="height: 100%" ref="homePieRef"></div>
				</div>
			</el-col>
		</el-row>
		<el-row :gutter="15" class="home-card-three">
			<el-col :xs="24" :sm="14" :md="14" :lg="8" :xl="8" class="home-media">
				<div class="home-card-item">
					<div style="height: 100%" ref="homeradarRef"></div>
				</div>
			</el-col>
			<el-col :xs="24" :sm="10" :md="10" :lg="16" :xl="16">
				<div class="home-card-item">
					<div class="home-card-item-title">实时检测信息
						<el-button type="primary" size="small" icon="Refresh" style="margin-left: 10px;" @click="refreshData">刷新数据</el-button>
					</div>
					<div class="home-monitor">
						<div class="flex-warp">
							<el-table :data="state.data" style="width: 100%">
								<el-table-column prop="username" label="用户名" align="center" />
								<el-table-column prop="anomalyScore" label="异常分数" align="center" />
								<el-table-column prop="isFault" label="故障状态" align="center">
									<template #default="scope">
										<el-tag :type="scope.row.isFault === 'true' ? 'danger' : 'success'">
											{{ scope.row.isFault === 'true' ? '异常' : '正常' }}
										</el-tag>
									</template>
								</el-table-column>
								<el-table-column prop="ai" label="AI助手" align="center">
									<template #default="scope">
										<span>{{ formatAIDisplay(scope.row.ai) }}</span>
									</template>
								</el-table-column>
								<el-table-column prop="checkType" label="检测类型" align="center">
									<template #default="scope">
										<el-tag :type="scope.row.checkType === '单点检测' ? 'primary' : 'success'">
											{{ scope.row.checkType }}
										</el-tag>
									</template>
								</el-table-column>
								<el-table-column prop="startTime" label="检测时间" align="center" />
							</el-table>
						</div>
					</div>
				</div>
			</el-col>
		</el-row>
		<el-row :gutter="15" class="home-card-three" style="margin-top: 15px;">
			<el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="24">
				<div class="home-card-item">
					<div style="height: 100%" ref="homeLineRef"></div>
					<!-- <div style="height: 100%" ref="homeradarRef"></div> -->
				</div>
			</el-col>
		</el-row>
	</div>
</template>

<script setup lang="ts" name="home">
import { reactive, onMounted, ref, watch, nextTick, onActivated, markRaw } from 'vue';
import * as echarts from 'echarts';
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '/@/stores/themeConfig';
import { useTagsViewRoutes } from '/@/stores/tagsViewRoutes';
import request from '/@/utils/request';
import axios from 'axios';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';

// 定义数据记录类型
interface RecordItem {
	id: number;
	username: string;
	anomalyScore: string;
	isFault: string;
	ai: string;
	startTime: string;
	batteryData: string | object;
	featureImportance?: string;
	checkType: string;
}

// 定义变量内容
const homeLineRef = ref();
const homePieRef = ref();
const homeBarRef = ref();
const homeradarRef = ref();
const storesTagsViewRoutes = useTagsViewRoutes();
const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);
const { isTagsViewCurrenFull } = storeToRefs(storesTagsViewRoutes);
const state = reactive({
	data: [] as RecordItem[],
	global: {
		homeChartOne: null,
		homeChartTwo: null,
		homeCharThree: null,
		homeCharFour: null,
		dispose: [null, '', undefined],
	} as any,
	myCharts: [] as EmptyArrayType,
	charts: {
		theme: '',
		bgColor: '',
		color: '#303133',
	},
});

// 折线图
const initLineChart = () => {
	if (!state.global.dispose.some((b: any) => b === state.global.homeChartOne)) {
		if (state.global.homeChartOne) state.global.homeChartOne.dispose();
	}
	state.global.homeChartOne = markRaw(echarts.init(homeLineRef.value, state.charts.theme));
	
	// 1. 按日期分组统计异常分数平均值
	const dateScores = {};
	const dateCounts = {};
	
	state.data.forEach(record => {
		if (!record.startTime) return;
		
		// 处理不同格式的日期 (2025/5/3 13:02:49 或 2025-05-03 13:02:49)
		let date;
		if (record.startTime.includes('/')) {
			// 提取日期部分 (2025/5/3)
			date = record.startTime.split(' ')[0];
			// 标准化日期格式 (确保始终使用相同的字符串作为键)
			const parts = date.split('/');
			if (parts.length === 3) {
				date = `${parts[0]}-${parts[1].padStart(2, '0')}-${parts[2].padStart(2, '0')}`;
			}
		} else {
			date = record.startTime.split(' ')[0];
		}
		
		const score = parseFloat(record.anomalyScore || '0');
		
		if (!isNaN(score)) {
			if (!dateScores[date]) {
				dateScores[date] = score;
				dateCounts[date] = 1;
			} else {
				dateScores[date] += score;
				dateCounts[date]++;
			}
		}
	});
	
	// 计算每日平均异常分数
	const avgScores = {};
	Object.keys(dateScores).forEach(date => {
		avgScores[date] = dateScores[date] / dateCounts[date];
	});

	// 2. 对所有日期按时间排序
	const sortedDates = Object.keys(avgScores).sort((a, b) => {
		const dateA = new Date(a.replace(/(\d+)\/(\d+)\/(\d+)/, '$1-$2-$3'));
		const dateB = new Date(b.replace(/(\d+)\/(\d+)\/(\d+)/, '$1-$2-$3'));
		return dateA.getTime() - dateB.getTime();
	});

	// 3. 取最近的10天数据，如果不足则全部使用
	const latest10Dates = sortedDates.slice(-10);

	// 4. 构造结果数据
	const result = {
		dateData: latest10Dates,
		valueData: latest10Dates.map(date => avgScores[date].toFixed(2))
	};

	// 确保有数据，如果没有则使用空数据
	if (result.dateData.length === 0) {
		const today = new Date();
		const dateStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
		result.dateData = [dateStr];
		result.valueData = ['0'];
	}
	
	const option = {
		backgroundColor: state.charts.bgColor,
		title: {
			text: '检测异常分数记录',
			x: 'left',
			textStyle: { fontSize: 15, color: state.charts.color },
		},
		grid: { top: 70, right: 20, bottom: 30, left: 40 },
		tooltip: { trigger: 'axis' },
		xAxis: {
			data: result.dateData,
			axisLabel: {
				color: '#000',
				rotate: 30
			},
		},
		yAxis: [
			{
				type: 'value',
				name: '异常分数',
				splitLine: { show: true, lineStyle: { type: 'dashed', color: '#f5f5f5' } },
				axisLabel: {
					color: '#000',
				},
			},
		],
		series: [
			{
				name: '异常分数',
				type: 'line',
				symbolSize: 6,
				symbol: 'circle',
				smooth: true,
				data: result.valueData,
				lineStyle: { color: '#9E87FF' },
				itemStyle: { color: '#9E87FF', borderColor: '#9E87FF' },
				areaStyle: {
					color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
						{ offset: 0, color: '#9E87FFb3' },
						{ offset: 1, color: '#9E87FF03' },
					]),
				},
				markLine: {
					silent: true,
					data: [{
						yAxis: 0.5,
						lineStyle: {
							color: '#ff4d4f',
							type: 'dashed'
						},
						label: {
							formatter: '警戒线(0.5)',
							position: 'end'
						}
					}]
				}
			},
		],
	};

	state.global.homeChartOne.setOption(option);
	state.myCharts.push(state.global.homeChartOne);
};

// 饼图
const initPieChart = () => {
	if (!state.global.dispose.some((b: any) => b === state.global.homeChartTwo)) {
		if (state.global.homeChartTwo) state.global.homeChartTwo.dispose();
	}
	state.global.homeChartTwo = markRaw(echarts.init(homePieRef.value, state.charts.theme));
	
	// 统计正常和异常状态的数量
	const faultStats = { '正常': 0, '异常': 0 };
	
	state.data.forEach(record => {
		// 解析故障状态，处理可能的字符串类型差异
		let isFault = false;
		if (typeof record.isFault === 'string') {
			isFault = record.isFault.toLowerCase() === 'true';
		} else if (typeof record.isFault === 'boolean') {
			isFault = record.isFault;
		}
		
		faultStats[isFault ? '异常' : '正常']++;
	});
	
	console.log('故障统计数据:', faultStats);

	// 构造饼图数据
	interface PieDataItem {
		name: string;
		value: number;
		labelLine?: any;
	}
	
	const pieData: PieDataItem[] = [];
	
	// 只在有数据时添加对应项
	if (faultStats['正常'] > 0) {
		pieData.push({ name: '正常', value: faultStats['正常'] });
	}
	
	if (faultStats['异常'] > 0) {
		pieData.push({ name: '异常', value: faultStats['异常'] });
	}
	
	// 如果完全没有数据，显示"无数据"
	if (pieData.length === 0) {
		pieData.push({ name: '无数据', value: 1 });
	}

	// 配置颜色
	const colorList = [
		{
			type: 'linear',
			x: 0,
			y: 0,
			x2: 1,
			y2: 1,
			colorStops: [
				{ offset: 0, color: 'rgba(51,192,205,0.3)' },
				{ offset: 1, color: 'rgba(51,192,205,0.8)' }
			],
			globalCoord: false
		},
		{
			type: 'linear',
			x: 0,
			y: 1,
			x2: 0,
			y2: 0,
			colorStops: [
				{ offset: 0, color: 'rgba(252,75,75,0.3)' },
				{ offset: 1, color: 'rgba(252,75,75,0.8)' }
			],
			globalCoord: false
		}
	];
	const colorLine = ['#33C0CD', '#FE6969'];

	// 为饼图中的富文本标签生成样式
	function getRich() {
		let rich = {};
		colorLine.forEach((v, i) => {
			rich[`hr${i}`] = {
				backgroundColor: colorLine[i],
				borderRadius: 3,
				width: 3,
				height: 3,
				padding: [3, 3, 0, -12]
			};
			rich[`a${i}`] = {
				padding: [-11, 6, -20, 6],
				color: colorLine[i],
				backgroundColor: 'transparent',
				fontSize: 12
			};
		});
		return rich;
	}

	// 给每个饼图数据设置 labelLine 的颜色
	pieData.forEach((v: any, i) => {
		v.labelLine = {
			lineStyle: {
				width: 1,
				color: colorLine[i % colorLine.length] // 确保不越界
			}
		};
	});

	// 配置饼图的 option
	const option = {
		backgroundColor: state.charts.bgColor,
		title: {
			text: '电池检测结果统计',
			x: 'left',
			textStyle: { fontSize: '15', color: state.charts.color },
		},
		legend: {
			top: 'bottom',
			data: pieData.map(item => item.name)
		},
		tooltip: {
			trigger: 'item',
			formatter: '{a} <br/>{b}: {c} ({d}%)'
		},
		series: [
			{
				name: '检测结果',
				type: 'pie',
				radius: '60%',
				center: ['50%', '50%'],
				clockwise: true,
				avoidLabelOverlap: true,
				label: {
					show: true,
					position: 'outside',
					formatter: function (params) {
						const name = params.name;
						const percent = params.percent + '%';
						const index = params.dataIndex;
						if (index >= 0 && index < colorLine.length) {
						return [`{a${index}|${name}：${percent}}`, `{hr${index}|}`].join('\n');
						} else {
							return `${name}：${percent}`;
						}
					},
					rich: getRich()
				},
				itemStyle: {
					normal: {
						color: function (params) {
							// 确保颜色索引不越界
							if (params.name === '正常') {
								return colorList[0];
							} else if (params.name === '异常') {
								return colorList[1];
							} else {
								// 其他类型（如'无数据'）使用灰色
								return {
									type: 'linear',
									x: 0, y: 0, x2: 1, y2: 0,
									colorStops: [
										{ offset: 0, color: 'rgba(180,180,180,0.3)' },
										{ offset: 1, color: 'rgba(180,180,180,0.8)' }
									]
								};
							}
						}
					}
				},
				data: pieData,
				emphasis: {
					itemStyle: {
						shadowBlur: 10,
						shadowOffsetX: 0,
						shadowColor: 'rgba(0, 0, 0, 0.5)'
					}
				}
			}
		]
	};

	state.global.homeChartTwo.setOption(option);
	state.myCharts.push(state.global.homeChartTwo);
};

const initradarChart = () => {
	if (!state.global.dispose.some((b: any) => b === state.global.homeCharFour)) {
		if (state.global.homeCharFour) state.global.homeCharFour.dispose();
	}
	state.global.homeCharFour = markRaw(echarts.init(homeradarRef.value, state.charts.theme));
	
	// 电池参数
	const batteryParams = ['总电压', '电流', '剩余电量', '最大单体电压', '最小单体电压', '最高温度', '最低温度'];
	
	// 计算真实的特征重要性分数
	let importanceScores = [85, 90, 75, 95, 92, 78, 70]; // 默认值
	
	// 尝试从数据中提取特征重要性
	const featuresData = state.data.filter(item => item.featureImportance);
	if (featuresData.length > 0) {
		try {
			const latestFeature = featuresData[0].featureImportance || '';
			if (latestFeature) {
				let parsedFeature;
				
				// 处理可能的字符串或已经是数组的情况
				if (typeof latestFeature === 'string') {
					parsedFeature = JSON.parse(latestFeature);
				} else if (Array.isArray(latestFeature)) {
					parsedFeature = latestFeature;
				}
				
				if (Array.isArray(parsedFeature) && parsedFeature.length >= 7) {
					// 转换为百分比值（0-100）
					importanceScores = parsedFeature.map(val => parseFloat(val) * 100);
					console.log('解析后的特征重要性:', importanceScores);
				}
			}
		} catch (e) {
			console.warn('无法解析特征重要性数据，使用默认值:', e);
		}
	}
	
	// 构造雷达图所需数据
	const indicator = batteryParams.map((name, idx) => ({ 
		name, 
		max: 100 
	}));

	// 构造系列数据
	const option = {
		backgroundColor: state.charts.bgColor,
		title: {
			text: '电池参数重要性分析',
			x: 'left',
			textStyle: { fontSize: '15', color: state.charts.color },
		},
		tooltip: {
			trigger: 'item',
			formatter: (params) => {
				const data = params.data.value;
				return batteryParams.map((param, i) => 
					`${param}: ${data[i].toFixed(2)}%`
				).join('<br>');
			}
		},
		radar: {
			radius: '70%',
			center: ['50%', '50%'],
			indicator: indicator,
			splitArea: {
				show: true,
				areaStyle: {
					color: ['#f5f5f5', '#e6e6e6']
				}
			},
			axisName: {
							color: '#333',
				fontSize: 12
			}
		},
		series: [{
			name: '参数重要性',
			type: 'radar',
			lineStyle: {
				width: 2,
				color: 'rgba(108,80,243,0.7)'
			},
			data: [{
				value: importanceScores,
				name: '参数重要性',
				areaStyle: {
					color: 'rgba(108,80,243,0.3)'
				},
				symbolSize: 6
			}]
		}]
	};

	state.global.homeCharFour.setOption(option);
	state.myCharts.push(state.global.homeCharFour);
};

// 柱状图
const initBarChart = () => {
	if (!state.global.dispose.some((b: any) => b === state.global.homeCharThree)) {
		if (state.global.homeCharThree) state.global.homeCharThree.dispose();
	}
	state.global.homeCharThree = markRaw(echarts.init(homeBarRef.value, state.charts.theme));
	
	// 统计每个用户的检测数量
	const userCounts = {};
	state.data.forEach(record => {
		const username = record.username || '未知用户';
		userCounts[username] = (userCounts[username] || 0) + 1;
		});

	// 转换为数组
	const sortedUsers = Object.keys(userCounts)
		.map(user => ({ name: user, count: userCounts[user] }))
		.sort((a, b) => b.count - a.count);
	
	// 如果没有数据，添加"无数据"项
	if (sortedUsers.length === 0) {
		sortedUsers.push({ name: '无数据', count: 0 });
			}
	
	const userNames = sortedUsers.map(item => item.name);
	const userValues = sortedUsers.map(item => item.count);
	
	const option = {
		backgroundColor: state.charts.bgColor,
		title: {
			text: '用户检测统计',
			x: 'left',
			textStyle: { fontSize: '15', color: state.charts.color },
		},
		tooltip: { 
			trigger: 'axis',
			formatter: '{b}: {c}次'
		},
		grid: { top: 70, right: 30, bottom: 50, left: 80 },
		xAxis: [
			{
				type: 'category',
				data: userNames,
				boundaryGap: true,
				axisTick: { show: false },
				axisLabel: {
					color: '#000',
					rotate: 30,
					interval: 0
				},
			},
		],
		yAxis: [
			{
				type: 'value',
				name: '检测次数',
				splitLine: { show: true, lineStyle: { type: 'dashed', color: '#f5f5f5' } },
				axisLabel: {
					color: '#000',
				},
			},
		],
		series: [
			{
				name: '检测次数',
				type: 'bar',
				barWidth: 30,
				itemStyle: {
					color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
						{ offset: 0, color: 'rgba(108,80,243,0.7)' },
						{ offset: 1, color: 'rgba(108,80,243,0.3)' },
					]),
					borderRadius: [30, 30, 0, 0],
				},
				data: userValues,
				label: {
					show: true,
					position: 'top',
					formatter: '{c}次'
				}
			},
		],
	};
	
	state.global.homeCharThree.setOption(option);
	state.myCharts.push(state.global.homeCharThree);
};

// 批量设置 echarts resize
const initEchartsResizeFun = () => {
	nextTick(() => {
		for (let i = 0; i < state.myCharts.length; i++) {
			setTimeout(() => {
				state.myCharts[i].resize();
			}, i * 1000);
		}
	});
};

// 批量设置 echarts resize
const initEchartsResize = () => {
	window.addEventListener('resize', initEchartsResizeFun);
};

// 添加刷新数据方法
const refreshData = () => {
	// 清空现有数据
	state.data = [];
	state.myCharts = [];
	
	// 重新获取数据
	fetchAllRecords();
};

// 提取数据获取逻辑为单独函数
const fetchAllRecords = () => {
	console.log('开始获取首页数据...');
	
	// 获取单点检测记录和时间段检测记录
	Promise.all([
		// 获取单点检测记录
		axios.get('http://localhost:9999/timepointRecords/all', {
			withCredentials: false,
			timeout: 10000
		}),
		// 获取时间段检测记录
		axios.get('http://localhost:9999/timeperiodRecords/all', {
			withCredentials: false,
			timeout: 10000
		})
	])
	.then(([timepointResponse, timeperiodResponse]) => {
		let allRecords: RecordItem[] = [];
		
		// 处理单点检测记录
		if (timepointResponse.status === 200 && timepointResponse.data) {
			console.log('单点检测API响应:', timepointResponse.data);
			
			// 强制将code转为数字类型进行比较
			const responseCode = parseInt(timepointResponse.data.code);
			if (responseCode === 0) {
				const timepointData = timepointResponse.data.data || [];
				console.log('成功获取单点检测记录，数量:', timepointData.length);
				
				// 处理单点检测数据
				const processedTimepointData = timepointData.map(item => {
					return {
						id: item.id,
						username: item.username || '未知用户',
						anomalyScore: item.anomalyScore || item.anomaly_score || '0',
						isFault: item.isFault || item.is_fault || 'false',
						ai: item.ai || '',
						startTime: item.startTime || item.start_time || '未知时间',
						batteryData: item.batteryData || item.battery_data || '{}',
						featureImportance: item.featureImportance || item.feature_importance || '',
						checkType: '单点检测'
					};
				}).filter(item => item.username && item.startTime);
				
				console.log('处理后的单点检测记录数:', processedTimepointData.length);
				allRecords = [...processedTimepointData];
			} else {
				console.warn('单点检测API返回错误码:', timepointResponse.data.code);
				// 不显示警告，避免干扰界面
			}
		} else {
			console.error('单点检测API请求失败', timepointResponse);
			// 不显示错误，避免干扰界面
		}
		
		// 处理时间段检测记录
		if (timeperiodResponse.status === 200 && timeperiodResponse.data) {
			console.log('时间段检测API响应:', timeperiodResponse.data);
			
			// 强制将code转为数字类型进行比较
			const responseCode = parseInt(timeperiodResponse.data.code);
			if (responseCode === 0) {
				const timeperiodData = timeperiodResponse.data.data || [];
				console.log('成功获取时间段检测记录，数量:', timeperiodData.length);
				
				// 处理时间段检测数据
				const processedTimeperiodData = timeperiodData.map(item => {
					return {
						id: item.id,
						username: item.username || '未知用户',
						anomalyScore: item.avgScore || item.avg_score || '0',  // 使用平均分数作为异常分数
						isFault: item.isFault || item.is_fault || 'false',
						ai: item.ai || '',
						startTime: item.startTime || item.start_time || '未知时间',
						batteryData: item.batteryData || item.battery_data || '{}',
						featureImportance: item.featureImportance || item.feature_importance || '',
						checkType: '时间段检测'
					};
				}).filter(item => item.username && item.startTime);
				
				console.log('处理后的时间段检测记录数:', processedTimeperiodData.length);
				// 合并数据
				allRecords = [...allRecords, ...processedTimeperiodData];
			} else {
				console.warn('时间段检测API返回错误码:', timeperiodResponse.data.code);
				// 不显示警告，避免干扰界面
			}
		} else {
			console.error('时间段检测API请求失败', timeperiodResponse);
			// 不显示错误，避免干扰界面
		}
		
		// 按照检测时间排序，较新的在前面
		allRecords.sort((a, b) => {
			// 处理不同格式的日期字符串
			let timeA = 0;
			let timeB = 0;
			
			try {
				// 处理"2025/5/3 13:02:49"格式
				if (a.startTime.includes('/')) {
					timeA = new Date(a.startTime.replace(/(\d+)\/(\d+)\/(\d+)/, '$3-$1-$2')).getTime();
				} else {
					timeA = new Date(a.startTime).getTime();
				}
				
				if (b.startTime.includes('/')) {
					timeB = new Date(b.startTime.replace(/(\d+)\/(\d+)\/(\d+)/, '$3-$1-$2')).getTime();
				} else {
					timeB = new Date(b.startTime).getTime();
				}
			} catch (e) {
				console.warn('日期解析错误:', e);
			}
			
			return timeB - timeA;
		});
		
		console.log('合并后的总检测记录数:', allRecords.length);
		
		// 更新状态
		state.data = allRecords;
		
		// 更新所有图表
		setTimeout(() => {
			initLineChart();
			initPieChart();
			initradarChart();
			initBarChart();
		}, 300);
		
		// 如果没有数据，显示警告（但不再显示弹窗，避免干扰）
		if (allRecords.length === 0) {
			console.warn('没有找到任何检测记录');
		}
	})
	.catch((error) => {
		console.error('获取首页数据出错:', error);
		// 不显示错误，避免干扰界面
		state.data = [];
		
		// 即使出错，也更新图表以显示真实情况
		setTimeout(() => {
			initLineChart();
			initPieChart();
			initradarChart();
			initBarChart();
		}, 300);
	})
	.finally(() => {
		// 确保图表初始化
	initEchartsResize();
});
};

// 页面加载时获取数据
onMounted(() => {
	console.log('首页组件已挂载，准备获取数据');
	fetchAllRecords();
});

// 由于页面缓存原因，keep-alive
onActivated(() => {
	console.log('首页组件已激活');
	initEchartsResizeFun();
});

// 监听 pinia 中的 tagsview 开启全屏变化，重新 resize 图表，防止不出现/大小不变等
watch(
	() => isTagsViewCurrenFull.value,
	() => {
		initEchartsResizeFun();
	}
);

// 监听 pinia 中是否开启深色主题
watch(
	() => themeConfig.value.isIsDark,
	(isIsDark) => {
		nextTick(() => {
			state.charts.theme = isIsDark ? 'dark' : '';
			state.charts.bgColor = isIsDark ? 'transparent' : '';
			state.charts.color = isIsDark ? '#dadada' : '#303133';
			setTimeout(() => {
				initLineChart();
				initradarChart();
				initPieChart();
				initBarChart();
			}, 500);
		});
	},
	{
		deep: true,
		immediate: true,
	}
);

// 格式化AI助手显示
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
</script>

<style scoped lang="scss">
$homeNavLengh: 8;

.home-container {
	overflow: hidden;

	.home-card-one,
	.home-card-two,
	.home-card-three {
		.home-card-item {
			width: 100%;
			height: 130px;
			border-radius: 4px;
			transition: all ease 0.3s;
			padding: 20px;
			overflow: hidden;
			background: var(--el-color-white);
			color: var(--el-text-color-primary);
			border: 1px solid var(--next-border-color-light);

			&:hover {
				box-shadow: 0 2px 12px var(--next-color-dark-hover);
				transition: all ease 0.3s;
			}

			&-icon {
				width: 70px;
				height: 70px;
				border-radius: 100%;
				flex-shrink: 1;

				i {
					color: var(--el-text-color-placeholder);
				}
			}

			&-title {
				font-size: 15px;
				font-weight: bold;
				height: 30px;
			}
		}
	}

	.home-card-one {
		@for $i from 0 through 3 {
			.home-one-animation#{$i} {
				opacity: 0;
				animation-name: error-num;
				animation-duration: 0.5s;
				animation-fill-mode: forwards;
				animation-delay: calc($i/4) + s;
			}
		}
	}

	.home-card-two,
	.home-card-three {
		.home-card-item {
			height: 400px;
			width: 100%;
			overflow: hidden;

			.home-monitor {
				height: 100%;

				.flex-warp-item {
					width: 25%;
					height: 111px;
					display: flex;

					.flex-warp-item-box {
						margin: auto;
						text-align: center;
						color: var(--el-text-color-primary);
						display: flex;
						border-radius: 5px;
						background: var(--next-bg-color);
						cursor: pointer;
						transition: all 0.3s ease;

						&:hover {
							background: var(--el-color-primary-light-9);
							transition: all 0.3s ease;
						}
					}

					@for $i from 0 through $homeNavLengh {
						.home-animation#{$i} {
							opacity: 0;
							animation-name: error-num;
							animation-duration: 0.5s;
							animation-fill-mode: forwards;
							animation-delay: calc($i/10) + s;
						}
					}
				}
			}
		}
	}
}
</style>
