/** 一次性脚本：从前端 mock 语料提取 5 段正文，生成后端 classpath 资源 */
import fs from 'node:fs'

const FRONT_TS = 'D:/AllProject/AiProject/AI-Write/front/src/api/mock/streamText.ts'
const src = fs.readFileSync(FRONT_TS, 'utf8')
const startMark = src.indexOf('= [')
const start = src.indexOf('[', startMark) + 1
const arrEnd = src.indexOf('];', start)
const body = src.slice(start, arrEnd)

/** 提取模板字符串 */
const segs = [...body.matchAll(/`([^`]*)`/g)].map((m) => m[1].trim())
if (segs.length !== 5) {
  throw new Error(`期望 5 段，实际 ${segs.length} 段`)
}

const total = segs.reduce((a, t) => a + (t.match(/[\u4e00-\u9fa5]/g) || []).length, 0)
const out = segs.join('\n\n--SEG--\n\n')

const dir = 'src/main/resources/novel'
fs.mkdirSync(dir, { recursive: true })
fs.writeFileSync(`${dir}/mock-chapter.txt`, out, 'utf8')

console.log('段数:', segs.length)
console.log('中文字数:', total)
console.log('输出:', dir + '/mock-chapter.txt')
