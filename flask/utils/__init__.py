import torch

def to_var(x):
    """将张量转换为变量"""
    if torch.cuda.is_available():
        x = x.cuda()
    return x

class Normalizer:
    """标准化工具类"""
    def __init__(self, mean=None, std=None):
        self.mean = mean
        self.std = std
    
    def fit(self, data):
        """计算均值和标准差"""
        self.mean = data.mean(axis=0)
        self.std = data.std(axis=0)
        self.std[self.std == 0] = 1.0  # 避免除以零
        return self
    
    def norm_func(self, data):
        """对数据进行标准化"""
        # 如果没有均值和标准差，使用输入数据计算
        if self.mean is None or self.std is None:
            self.fit(data)
            
        # 标准化处理
        normalized = (data - self.mean) / self.std
        return normalized
    
    def denorm_func(self, normalized_data):
        """反标准化处理"""
        if self.mean is None or self.std is None:
            raise ValueError("需要先调用fit或norm_func方法设置均值和标准差")
            
        denormalized = normalized_data * self.std + self.mean
        return denormalized 