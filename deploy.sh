#!/bin/bash
# 泡泡后端部署脚本
set -e

REMOTE="ubuntu@118.25.186.221"
SSH_KEY="$HOME/.ssh/bubble-server"
REMOTE_DIR="/home/ubuntu/bubble"
SSH="ssh -i $SSH_KEY -o StrictHostKeyChecking=no"
SCP="scp -i $SSH_KEY -o StrictHostKeyChecking=no"

echo "==> 同步代码到服务器..."
$SSH $REMOTE "mkdir -p $REMOTE_DIR/agent $REMOTE_DIR/gateway"

# tar 打包上传（排除本地敏感文件和不需要的目录）
tar cf - \
  --exclude='node_modules' \
  --exclude='.git' \
  --exclude='phone' \
  --exclude='*.keystore' \
  --exclude='.env' \
  agent/ gateway/ | \
  $SSH $REMOTE "tar xf - -C $REMOTE_DIR/"

echo "==> 上传 .env（本地，不入 git）..."
if [ -f gateway/.env ]; then
  $SCP gateway/.env $REMOTE:$REMOTE_DIR/gateway/.env
else
  echo "⚠️  gateway/.env 不存在！服务将无法调用 AI。"
  echo "   请创建 gateway/.env（参考 gateway/.env.example）"
  exit 1
fi

echo "==> 安装 agent 依赖..."
$SSH $REMOTE "cd $REMOTE_DIR/agent && npm install --omit=dev"

echo "==> 安装 gateway 依赖..."
$SSH $REMOTE "cd $REMOTE_DIR/gateway && npm install"

echo "==> 配置 systemd 服务..."
$SSH $REMOTE "sudo tee /etc/systemd/system/bubble.service > /dev/null" << 'UNIT'
[Unit]
Description=Bubble Agent Gateway
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/bubble/gateway
Environment=NODE_ENV=production
ExecStart=/usr/bin/npx tsx --env-file=.env server.ts
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
UNIT

echo "==> 启动服务..."
$SSH $REMOTE "sudo systemctl daemon-reload && sudo systemctl enable bubble && sudo systemctl restart bubble"

echo "==> 等待启动..."
sleep 3

echo "==> 检查状态..."
$SSH $REMOTE "sudo systemctl status bubble --no-pager -l"

echo ""
echo "✅ 部署完成！WebSocket: ws://118.25.186.221:8080"
