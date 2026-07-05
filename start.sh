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

echo "后端启动中，最多等待 60 秒..."

# 3. 轮询检查启动结果
ok=false
for i in $(seq 1 60); do
  if grep -q "Started CampusRunnerApplication" campus-runner.log 2>/dev/null; then
    echo "✅ 后端启动成功（耗时约 ${i} 秒）"
    ok=true
    break
  fi
  if grep -qE "APPLICATION FAILED TO START|Error starting ApplicationContext|Caused by:" campus-runner.log 2>/dev/null; then
    echo "❌ 后端启动失败，查看日志："
    tail -40 campus-runner.log
    break
  fi
  sleep 1
done

if [ "$ok" = "false" ] && ! grep -q "Started CampusRunnerApplication" campus-runner.log 2>/dev/null; then
  echo "⚠️  60 秒内未检测到启动成功标志，最近日志："
  tail -40 campus-runner.log
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
