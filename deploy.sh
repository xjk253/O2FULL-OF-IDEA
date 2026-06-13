#!/bin/bash
# 泡泡后端部署脚本
set -e

REMOTE="ubuntu@118.25.186.221"
SSH_KEY="$HOME/.ssh/bubble-server"
REMOTE_DIR="/home/ubuntu/bubble"
SSH="ssh -i $SSH_KEY -o StrictHostKeyChecking=no"

echo "==> 清理旧代码 & 上传..."
$SSH $REMOTE "rm -rf $REMOTE_DIR && mkdir -p $REMOTE_DIR/agent $REMOTE_DIR/gateway"

# tar 打包上传
tar cf - \
  --exclude='node_modules' \
  --exclude='.git' \
  --exclude='phone' \
  --exclude='*.keystore' \
  agent/ gateway/ agent/ | \
  $SSH $REMOTE "tar xf - -C $REMOTE_DIR/"

echo "==> 安装 agent 依赖..."
$SSH $REMOTE "cd $REMOTE_DIR/agent && npm install --omit=dev"

echo "==> 安装 gateway 依赖 & 构建..."
$SSH $REMOTE "cd $REMOTE_DIR/gateway && npm install && npm run build"

echo "==> 配置 systemd 服务..."
$SSH $REMOTE "sudo tee /etc/systemd/system/bubble.service > /dev/null" << 'UNIT'
[Unit]
Description=Bubble Agent Gateway
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/bubble/gateway
Environment=BUBBLE_API_KEY=649454e74b4f409f92186ca85ad2c7e7.nx9SnFWFfI7CEajR
Environment=BUBBLE_BASE_URL=https://open.bigmodel.cn/api/anthropic
Environment=BUBBLE_MODEL=glm-4-flash
Environment=PORT=8080
Environment=NODE_ENV=production
ExecStart=/usr/bin/node dist/server.js
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
