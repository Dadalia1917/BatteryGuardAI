import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Session } from '/@/utils/storage';
import qs from 'qs';

// 配置新建一个 axios 实例
const service: AxiosInstance = axios.create({
	baseURL: import.meta.env.VITE_API_DOMAIN,
	timeout: 500000,
	headers: { 'Content-Type': 'application/json;charset=UTF-8' },
	withCredentials: false, // 关闭跨域请求时携带凭据，与后端CORS配置保持一致
});

// 添加请求拦截器
service.interceptors.request.use(
	(config: AxiosRequestConfig) => {
		// 在发送请求之前做些什么 token
		if (Session.get('token')) {
			config.headers!['Authorization'] = `${Session.get('token')}`;
		}
		// 调试日志
		console.log('【请求拦截】发送请求:', config.url, '请求方法:', config.method, '数据:', config.data || config.params);
		return config;
	},
	(error) => {
		// 对请求错误做些什么
		console.error('【请求拦截】请求错误:', error);
		return Promise.reject(error);
	}
);

// 添加响应拦截器
service.interceptors.response.use(
	(response) => {
		// 对响应数据做点什么
		let res = response.data;
		// 添加响应日志
		console.log('【响应拦截】收到响应:', response.config.url, '状态:', response.status, '数据类型:', typeof res);
		
		// 处理字符串返回值，尝试解析为JSON
		if (typeof res === 'string') {
			try {
				res = JSON.parse(res);
				console.log('【响应拦截】成功将字符串响应解析为JSON');
			} catch (error) {
				console.error('【响应拦截】响应字符串解析失败，将直接返回:', error);
			}
		}
		
		if (res.code === 401 || res.code === 4001) {
			Session.clear(); // 清除浏览器全部临时缓存
			window.location.href = '/'; // 去登录页
			ElMessageBox.alert('你已被登出，请重新登录', '提示', {})
				.then(() => {})
				.catch(() => {});
		} else {
			return res;
		}
	},
	(error) => {
		// 对响应错误做点什么
		console.error('【响应拦截】请求错误:', error);
		if (error.response) {
			console.error('【响应拦截】错误响应数据:', error.response.data);
			console.error('【响应拦截】错误状态码:', error.response.status);
			console.error('【响应拦截】请求配置:', error.config);
		}
		
		if (error.message.indexOf('timeout') != -1) {
			ElMessage.error('网络超时');
		} else if (error.message == 'Network Error') {
			ElMessage.error('网络连接错误');
		} else {
			if (error.response && error.response.data) ElMessage.error(error.response.statusText);
			else ElMessage.error('接口路径找不到');
		}
		return Promise.reject(error);
	}
);

// 导出 axios 实例
export default service;
