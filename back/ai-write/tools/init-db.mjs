/**
 * 建库 + 执行 schema.sql
 * 用法：node tools/init-db.mjs [user] [password]
 * 需要本机 MySQL 的 mysql 客户端在 PATH 中；若不在，请手动执行：
 *   mysql -u root -p < src/main/resources/schema.sql
 */
import { execFileSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

const user = process.argv[2] || 'root'
const password = process.argv[3] || 'root'
const schema = path.resolve('src/main/resources/schema.sql')

const sql = `CREATE DATABASE IF NOT EXISTS ai_write DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;`
fs.writeFileSync('/tmp/_create_db.sql', sql, 'utf8')

function run(args) {
  return execFileSync('mysql', args, { encoding: 'utf8' })
}

try {
  run(['-u', user, `-p${password}`, '-e', sql])
  console.log('✓ 数据库 ai_write 已就绪')
  run(['-u', user, `-p${password}`, 'ai_write', '-e', `source ${schema}`])
  console.log('✓ schema.sql 执行完成')
  const tables = run(['-u', user, `-p${password}`, 'ai_write', '-e', 'SHOW TABLES;'])
  console.log(tables)
} catch (e) {
  console.error('✗ 执行失败：', e.message)
  console.error('mysql 客户端可能不在 PATH，请手动执行：')
  console.error(`  mysql -u ${user} -p < src/main/resources/schema.sql`)
  process.exit(1)
}
