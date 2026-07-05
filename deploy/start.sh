#!/bin/bash
# CampusRunner 开机启动脚本
# 用法：bash start.sh

echo "=== 正在启动 CampusRunner ==="

# 1. 杀掉可能残留的旧进程
fuser -k 8080/tcp 2>/dev/null
sleep 2

# 2. 启动后端
cd /app/campus_runner

# 从环境变量读取数据库密码（优先级：环境变量 > 默认值）
DB_PWD="${DB_PWD:-a1234567}"

# AI Agent 配置（避免重启后环境变量丢失导致 AI 不可用）
HP_AI_AGENT_KEY="${HP_AI_AGENT_KEY:-sk-2oCOerZqGTIzGmmw5XQj2iiVhZICBqzF}"
HP_AI_AGENT_BASE_URL="${HP_AI_AGENT_BASE_URL:-https://token.sensenova.cn/v1/chat/completions}"
HP_AI_AGENT_MODEL="${HP_AI_AGENT_MODEL:-deepseek-v4-flash}"

nohup java -jar campus-runner-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --campus.upload.path=/app/campus_runner/uploads \
  --spring.datasource.password="${DB_PWD}" \
  --hp.ai.agent.key="${HP_AI_AGENT_KEY}" \
  --hp.ai.agent.base-url="${HP_AI_AGENT_BASE_URL}" \
  --hp.ai.agent.model="${HP_AI_AGENT_MODEL}" \
  > campus-runner.log 2>&1 &

echo "后端启动中，等待 5 秒..."
sleep 5

# 3. 检查启动结果
if grep -q "Started CampusRunnerApplication" campus-runner.log 2>/dev/null; then
  echo "✅ 后端启动成功"
else
  echo "❌ 后端可能启动失败，查看日志："
  tail -20 campus-runner.log
fi

# 4. 确保 Nginx 在运行
if pgrep nginx > /dev/null; then
  echo "✅ Nginx 已运行"
else
  echo "🔄 启动 Nginx..."
  nginx
  echo "✅ Nginx 已启动"
fi

echo ""
echo "=== 启动完成 ==="
echo "前端：http://你的服务器IP/"
echo "后端：http://你的服务器IP:8080/"
