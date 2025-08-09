import pickle  # 添加此行导入pickle模块

with open("dyad_vae_save/2025-01-03-15-12-44_fold4/model/norm.pkl", "rb") as f:
    normalizer = pickle.load(f)
    print("均值形状:", normalizer.mean.shape)   # 预期输出 (7,)
    print("标准差形状:", normalizer.std.shape)  # 预期输出 (7,)