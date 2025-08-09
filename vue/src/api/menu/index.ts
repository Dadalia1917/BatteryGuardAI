import request from '/@/utils/request';

/**
 * 以下为模拟接口地址
 * 
 * （不建议写成 request.post(xxx)，因为这样 post 时，无法 params 与 data 同时传参）
 * 
 * 后端控制路由，isRequestRoutes 为 true，则开启后端控制路由
 * @method getAdminMenu 获取后端动态路由菜单(admin)
 * @method getTestMenu 获取后端动态路由菜单(test)
 */
export function useMenuApi() {
	return {
		getAdminMenu: (params?: object) => {
			return request({
				url: '/api/menu/admin',
				method: 'get',
				params,
			});
		},
		getTestMenu: (params?: object) => {
			return request({
				url: '/api/menu/test',
				method: 'get',
				params,
			});
		},
	};
}
