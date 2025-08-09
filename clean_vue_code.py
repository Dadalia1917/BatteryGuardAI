import os
import re

def clean_file(file_path):
    """清理文件中的gitee.com链接和作者信息"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 替换所有包含gitee.com链接的注释行
    cleaned_content = re.sub(r'// https?://gitee\.com/lyt-top/vue-next-admin.*\n', '', content)
    cleaned_content = re.sub(r'// 修复：https?://gitee\.com/lyt-top/vue-next-admin.*\n', '', cleaned_content)
    cleaned_content = re.sub(r'// 拖动问题，https?://gitee\.com/lyt-top/vue-next-admin.*\n', '', cleaned_content)
    
    if content != cleaned_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(cleaned_content)
        print(f"已清理: {file_path}")

def process_directory(directory):
    """处理目录中的所有.vue和.ts文件"""
    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith(('.vue', '.ts')):
                file_path = os.path.join(root, file)
                clean_file(file_path)

if __name__ == "__main__":
    vue_dir = "./vue/src"
    if os.path.exists(vue_dir):
        process_directory(vue_dir)
        print("Vue代码清理完成！")
    else:
        print(f"目录不存在: {vue_dir}") 