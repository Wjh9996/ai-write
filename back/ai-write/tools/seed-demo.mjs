const BASE = 'http://127.0.0.1:8080'
const res = await fetch(BASE + '/api/auth/login', {
  method: 'POST', headers: {'Content-Type':'application/json'},
  body: JSON.stringify({email:'author@ai-write.dev', password:'123456'})
})
const token = (await res.json()).data.token
const H = {'Content-Type':'application/json', Authorization:'Bearer '+token}

const OUTLINE = `【主线】开局在废矿捡到一枚灰蛋 → 孵化出第一只兽宠 → 参加宝宝赛 → 晋升高级学府 → 战异兽皇 → 成为扬名世界的御兽大师。

【第一卷·废矿里的灰蛋】林昭被发配看守废弃矿洞，在矿洞深处发现一枚无人认领的灰蛋，靠半吊子驯兽知识把它孵出来，带着这只奇怪的小东西走出矿镇，第一次接触真正的御兽世界。`

const n = await (await fetch(BASE+'/api/novels', {method:'POST', headers:H,
  body: JSON.stringify({title:'御兽：从一枚灰蛋开始', intro:'一个被发配守废矿的少年，从一枚灰蛋开始成为御兽大师。', mainOutline: OUTLINE})})).json()
const novelId = n.data.id
console.log('作品 id =', novelId)

await fetch(BASE+`/api/novels/${novelId}/outline`, {method:'PUT', headers:H, body: JSON.stringify({mainOutline: OUTLINE})})

const vols = (await (await fetch(BASE+`/api/novels/${novelId}/volumes`, {headers:H})).json()).data
const vol = vols[0]
await fetch(BASE+`/api/volumes/${vol.id}/events`, {method:'PUT', headers:H, body: JSON.stringify({events:[
  '林昭被族里发配去看守废弃矿洞',
  '在矿洞深处发现一枚无人认领的灰蛋',
  '尝试孵化失败三次，差点被自己冻死',
  '用体温和最后一袋灵米把蛋孵了出来',
  '幼兽破壳，认主，取名灰羽',
  '带灰羽第一次走出矿镇，被镇上嘲笑',
  '遭遇第一只野生岩甲蟹，被迫实战',
  '矿镇小比，赢下第一枚灵石',
  '卷末钩子：招生官的车停在了矿镇口'
]})})

const gen = await (await fetch(BASE+`/api/volumes/${vol.id}/outline-generation`, {method:'POST', headers:H})).json()
console.log('拆出章纲', gen.data.outlines.length, '条')
await fetch(BASE+`/api/volumes/${vol.id}/outlines/confirm`, {method:'PUT', headers:H})
const chapters = (await (await fetch(BASE+`/api/novels/${novelId}/chapters`, {headers:H})).json()).data
console.log('章节', chapters.length, '章，第一章：', chapters[0].title)

// 设定库
await fetch(BASE+`/api/novels/${novelId}/settings/extract`, {method:'POST', headers:H})
const st = (await (await fetch(BASE+`/api/novels/${novelId}/settings`, {headers:H})).json()).data
console.log('设定实体', st.entities.length, '条')
console.log('完成，打开前端即可看到')
