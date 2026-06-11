#!/bin/bash

cd "$(dirname "$0")"

if [ ! -f .env ]; then
    echo "⚠️  未找到 .env 文件，正在从 .env.example 创建..."
    cp .env.example .env
    echo "✅ 已创建 .env 文件，请修改其中的密码配置后再启动"
    exit 1
fi

echo "🚀 正在启动社区充电车棚管理系统..."

docker compose up -d --build

echo ""
echo "⏳ 等待服务启动中..."
sleep 5

echo ""
echo "📋 服务状态："
docker compose ps

echo ""
echo "✅ 启动完成！"
echo "🌐 前端访问: http://localhost:${FRONTEND_PORT:-80}"
echo "🔌 后端API:  http://localhost:${BACKEND_PORT:-8080}/api"
echo "📊 健康检查: http://localhost:${BACKEND_PORT:-8080}/api/public/health"
echo ""
echo "🔑 测试账号："
echo "   居民:   resident / 123456"
echo "   物业:   property / 123456"
echo "   安全员: safety / 123456"
echo ""
echo "📝 查看日志: docker compose logs -f [backend|frontend|mysql]"
echo "⏹️  停止服务: docker compose down"
