import type {
  Chapter,
  ChapterOutline,
  Foreshadowing,
  Issue,
  Novel,
  SettingChangeDraft,
  SettingEntity,
  StyleProfile,
  TimelineEvent,
  UsageRecord,
  Volume,
  WorldSetting
} from '@/types'

/**
 * Mock 种子数据
 * 语料取自《设计文档NEW.md》第 8 节测试 fixture：《御兽：从一枚灰蛋开始》
 */

const NOW = '2026-10-31T10:00:00'

const SAMPLE_MAIN_OUTLINE = `【主线】开局在废矿捡到一枚灰蛋 → 孵化出第一只兽宠 → 参加宝宝赛 → 晋升高级学府 → 学府大比 → 世界裂缝扩散，异兽袭击 → 战场生死危机 → 战异兽皇 → 最终之战 → 成为扬名世界的御兽大师。

【第一卷·废矿】主角林昭在家族废弃矿洞里发现一枚无人认领的灰蛋，靠半吊子驯兽知识把它孵出来，带着这只奇怪的小东西走出矿镇，第一次接触真正的御兽世界。

【第二卷·宝宝赛】初入低级学府测试场，以"灰羽"参加全国宝宝赛，靠一次近乎犯规的临场判断拿下名次，也因此被高级学府的招生官盯上。

【第三卷·学府】进入青崖高级学府，加入 Eh?……加入"杂班"，遭遇旧势力排挤，结识挚友与宿敌，学分赛上一路逆袭。

【第四卷·裂缝】世界裂缝扩散，异兽开始袭击人类聚居地，主角第一次上战场，在生死边缘完成第二次契约。

【第五卷·异兽皇】战线崩溃，异兽皇现世，主角团付出惨重代价守住断后线，挚友重伤，主角觉醒第三契约。

【第六卷·终战】集结全部伙伴发动总攻，在 worlds 尽头击杀异兽皇，成为扬名世界的御兽大师。

【第七卷·余响】战后重建，主角回到废矿，把那枚蛋壳埋在原处，收了一个和自己当年一样穷的学徒。`

const VOLUME_EVENTS: Record<number, string[]> = {
  1: [
    '林昭被族里发配去看守废弃矿洞',
    '在矿洞深处发现一枚无人认领的灰蛋',
    '尝试孵化失败三次，差点被自己冻死',
    '用体温和最后一袋灵米把蛋孵了出来',
    '幼兽破壳，认主，取名"灰羽"',
    '带灰羽第一次走出矿镇，被镇上嘲笑',
    '遭遇第一只野生岩甲蟹，被迫实战',
    '矿镇小比，赢下第一枚灵石',
    '卷末钩子：招生官的车停在了矿镇口'
  ],
  2: [
    '抵达低级学府测试场，资质评定垫底',
    '报名全国宝宝赛，被判定为送分名额',
    '首轮对战第一学府的天才',
    '临场判断：让灰羽假死骗过对手的预判',
    '爆冷晋级十六强',
    '八强赛被针对，灰羽受伤',
    '决赛以微弱差距落败，拿到第二',
    '青崖高级学府招生官当场递出特招令'
  ],
  3: [
    '入学青崖，被分进被称为"垃圾场"的丙字班',
    '认识同桌苏晚与宿敌裴临',
    '第一次学分赛，丙字班垫底',
    '发现灰羽的"拟态"能力隐藏的第二阶段',
    '中期考核逆袭，进入甲字班视野',
    '期末大比，苏晚为救林昭受伤',
    '林昭以第七名收官，获得进入禁地试炼资格'
  ]
}

function buildVolumes(novelId: number): Volume[] {
  return [
    {
      id: novelId * 100 + 1,
      novelId,
      volumeNo: 1,
      title: '第一卷 · 废矿里的灰蛋',
      events: VOLUME_EVENTS[1].map((content, i) => ({ id: 11 + i, sort: i + 1, content })),
      status: 'CONFIRMED',
      targetChapters: 18,
      createdAt: NOW
    },
    {
      id: novelId * 100 + 2,
      novelId,
      volumeNo: 2,
      title: '第二卷 · 宝宝赛',
      events: VOLUME_EVENTS[2].map((content, i) => ({ id: 21 + i, sort: i + 1, content })),
      status: 'PENDING_CONFIRM',
      targetChapters: 16,
      createdAt: NOW
    },
    {
      id: novelId * 100 + 3,
      novelId,
      volumeNo: 3,
      title: '第三卷 · 青崖学府',
      events: VOLUME_EVENTS[3].map((content, i) => ({ id: 31 + i, sort: i + 1, content })),
      status: 'RAW',
      targetChapters: 20,
      createdAt: NOW
    }
  ]
}

function buildEntities(novelId: number): SettingEntity[] {
  return [
    {
      id: 1,
      novelId,
      type: 'CHARACTER',
      name: '林昭',
      aliases: ['主角', '小昭'],
      card: {
        身份: '旁系子弟，被发配守废矿',
        年龄: '16',
        性格: '闷，认死理，一旦认准就不松口',
        外貌: '瘦，黑，右手掌心有一层洗不掉的矿灰',
        口癖: '"……先试试。"',
        当前等级: '御兽学徒 · 一阶',
        契约兽: '灰羽'
      },
      source: 'MANUAL',
      approvalStatus: 'CONFIRMED',
      revealChapter: 1,
      updatedAt: NOW
    },
    {
      id: 2,
      novelId,
      type: 'CHARACTER',
      name: '灰羽',
      aliases: ['小东西', '灰团子'],
      card: {
        身份: '林昭的第一只契约兽',
        种族: '未知（蛋壳无纹）',
        阶段: '幼生期',
        能力: '拟态 · 一阶段：复制接触过的非生命物质质感',
        口癖: '"啾。"',
        禁忌: '不得提前解释其血脉来源'
      },
      source: 'AI',
      approvalStatus: 'CONFIRMED',
      revealChapter: 5,
      updatedAt: NOW
    },
    {
      id: 3,
      novelId,
      type: 'CHARACTER',
      name: '苏晚',
      aliases: ['同桌'],
      card: {
        身份: '青崖丙字班，后来转甲班',
        性格: '嘴毒心软',
        契约兽: '素心螟',
        备注: '第三卷末为救林昭受伤'
      },
      source: 'AI',
      approvalStatus: 'DRAFT',
      revealChapter: 20,
      updatedAt: NOW
    },
    {
      id: 4,
      novelId,
      type: 'CHARACTER',
      name: '裴临',
      aliases: [],
      card: {
        身份: '甲字班首席，裴家嫡子',
        关系: '宿敌，后来是战友',
        契约兽: '赤鳞蛟'
      },
      source: 'AI',
      approvalStatus: 'DRAFT',
      revealChapter: 22,
      updatedAt: NOW
    },
    {
      id: 5,
      novelId,
      type: 'PLACE',
      name: '黑石废矿',
      aliases: ['废矿'],
      card: {
        位置: '林家领地最西端，靠着断崖',
        特点: '矿脉枯竭，常年渗水，洞壁有不明抓痕',
        备注: '灰蛋发现地点：三号巷道尽头的水潭边'
      },
      source: 'MANUAL',
      approvalStatus: 'CONFIRMED',
      revealChapter: 1,
      updatedAt: NOW
    },
    {
      id: 6,
      novelId,
      type: 'ITEM',
      name: '净息符',
      aliases: [],
      card: {
        作用: '压制灵力波动',
        备注: '第七章才能出现，第一章严禁提及'
      },
      source: 'MANUAL',
      approvalStatus: 'CONFIRMED',
      revealChapter: 7,
      updatedAt: NOW
    }
  ]
}

function buildWorlds(novelId: number): WorldSetting[] {
  return [
    {
      id: 1,
      novelId,
      category: '世界规则',
      title: '契约规则',
      content:
        '契约不可逆。契约成立的瞬间，御兽师与兽宠共享部分感知，也共担伤害。契约兽死亡，御兽师轻则废掉半身灵脉，重则当场毙命。',
      source: 'MANUAL',
      approvalStatus: 'CONFIRMED'
    },
    {
      id: 2,
      novelId,
      category: '世界规则',
      title: '境界划分',
      content: '学徒 → 御者 → 御师 → 大御师 → 御王 → 御皇。每一阶分九段。',
      source: 'MANUAL',
      approvalStatus: 'CONFIRMED'
    },
    {
      id: 3,
      novelId,
      category: '地理',
      title: '世界裂缝',
      content:
        '位于大陆北端的巨型裂隙，异兽潮的源头。第四卷开始扩散。当前阶段普通人不知道裂缝的存在。',
      source: 'AI',
      approvalStatus: 'CONFIRMED'
    },
    {
      id: 4,
      novelId,
      category: '势力',
      title: '青崖高级学府',
      content: '东境最高学府，分甲乙丙三班。丙字班俗称"垃圾场"。',
      source: 'AI',
      approvalStatus: 'DRAFT'
    }
  ]
}

function buildTimeline(novelId: number): TimelineEvent[] {
  return [
    {
      id: 1,
      novelId,
      chapterId: 1,
      timeLabel: '天启历 982 年 · 冬月末',
      event: '林昭被发配看守黑石废矿',
      approvalStatus: 'CONFIRMED',
      sort: 1
    },
    {
      id: 2,
      novelId,
      chapterId: 5,
      timeLabel: '天启历 982 年 · 腊月初',
      event: '灰羽破壳并认主',
      approvalStatus: 'CONFIRMED',
      sort: 2
    },
    {
      id: 3,
      novelId,
      chapterId: 9,
      timeLabel: '天启历 982 年 · 腊月十七',
      event: '第一次实战，击退野生岩甲蟹',
      approvalStatus: 'CONFIRMED',
      sort: 3
    },
    {
      id: 4,
      novelId,
      chapterId: 18,
      timeLabel: '天启历 983 年 · 正月',
      event: '青崖招生官抵达矿镇',
      approvalStatus: 'CONFIRMED',
      sort: 4
    }
  ]
}

function buildForeshadowings(novelId: number): Foreshadowing[] {
  return [
    {
      id: 1,
      novelId,
      content: '三号巷道尽头的洞壁上有五道抓痕，比人手宽一倍',
      setChapterId: 2,
      payoffChapterId: null,
      status: 'PLANTED',
      source: 'MANUAL',
      approvalStatus: 'CONFIRMED'
    },
    {
      id: 2,
      novelId,
      content: '林昭掌心洗不掉的灰 —— 后来证明是蛋壳粉末渗进皮肤',
      setChapterId: 1,
      payoffChapterId: null,
      status: 'PLANTED',
      source: 'MANUAL',
      approvalStatus: 'CONFIRMED'
    },
    {
      id: 3,
      novelId,
      content: '镇上老驯兽师说了一句"这纹路……不对，太素了"',
      setChapterId: 6,
      payoffChapterId: null,
      status: 'PLANTED',
      source: 'AI',
      approvalStatus: 'DRAFT'
    }
  ]
}

function buildStyle(novelId: number): StyleProfile {
  return {
    id: 1,
    novelId,
    card: {
      人称: '第三人称',
      视角: '限知视角（跟随林昭）',
      节奏: '两章铺垫一章爆发，每章至少一个钩子',
      基调: '正能量、积极向上，但要有重量，不廉价的热血',
      对话比例: '30%—35%',
      句长: '短句为主，动作场景用极短句',
      爽点: '以弱胜强、被轻视后的正名、伙伴羁绊',
      禁忌: '不写虐心虐身；不写暧昧多女主；不出现现代词汇'
    },
    sampleText:
      '矿洞里的水一直在滴。\n\n林昭数到第三百二十七滴的时候，掌心底下那枚蛋动了一下。\n\n很轻，像有人在里头翻了个身。\n\n他屏住呼吸，把耳朵贴上去。里面静了半晌，又动了一下。\n\n"……活的。"\n\n他说出这两个字的时候，嗓子哑得不像自己的。',
    forbiddenTerms: ['上帝视角', '系统', '金手指', '女主']
  }
}

const OUTLINE_TITLES = [
  '废矿里的灰蛋',
  '三号巷道',
  '掌心里的温度',
  '第三次失败',
  '破壳',
  '啾',
  '老驯兽师的话',
  '走出矿镇',
  '岩甲蟹',
  '十四枚灵石'
]

function buildOutlines(novelId: number): ChapterOutline[] {
  const volumeId = novelId * 100 + 1
  const beatsPool = [
    ['林昭在废矿清点工具', '发现巷道深处的反光', '决定独自下到三号巷道尽头'],
    ['描述三号巷道的渗水与寂静', '水潭边那枚灰蛋第一次出现', '林昭伸手拿起来时的触感'],
    ['回工棚查资料', '确认这不是任何已知兽卵', '决定试着孵化'],
    ['第一次孵化失败，蛋变冷', '第二次失败', '林昭把最后半袋灵米碾成粉'],
    ['第三次尝试，用体温捂了一整夜', '蛋壳出现第一道裂纹', '林昭昏睡过去'],
    ['破壳瞬间，一只湿漉漉的幼兽', '对视，认主契约成形', '林昭给它取名灰羽'],
    ['带灰羽去见镇上的老驯兽师', '老驯兽师看了一眼蛋壳说"纹路太素"', '拒绝解释含义，埋下钩子'],
    ['林昭带灰羽走出矿镇', '被同龄人嘲笑捡了个废品', '灰羽第一次展露拟态'],
    ['回程路上遭遇野生岩甲蟹', '灰羽受伤，林昭以肉身挡下一次钳击', '用地形反杀'],
    ['矿镇小比报名', '连胜三轮', '赢下第一枚灵石，决定离开矿镇']
  ]
  return OUTLINE_TITLES.map((title, i) => ({
    id: volumeId * 1000 + i + 1,
    volumeId,
    chapterNo: i + 1,
    title: `${i + 1}. ${title}`,
    beats: beatsPool[i] ?? ['推进本卷主线的关键一步', '与已有角色互动', '章末留下钩子'],
    characters: i === 0 ? ['林昭'] : ['林昭', '灰羽'],
    estimatedWords: 2500,
    status: i < 9 ? 'CONFIRMED' : 'DRAFT',
    version: 1
  }))
}

const CHAPTER1_SAMPLE = `矿洞里的水一直在滴。

林昭数到第三百二十七滴的时候，掌心底下那枚蛋动了一下。

很轻，像有人在里头翻了个身。

他屏住呼吸，把耳朵贴上去。里面静了半晌，又动了一下。

"……活的。"

他说出这两个字的时候，嗓子哑得不像自己的。

三号巷道是黑石废矿深处最偏的一条。老人们说那里早年挖出过东西，后来塌了半边，族里就把口子封了，只留一条能过人的缝隙。林昭是被族里发配来看矿的，名义上是"管事"，实际上连个帮他挑水的都没有。

他蹲在水潭边上，用袖子把蛋上的泥擦掉。

蛋不大，比拳头略长，灰扑扑的，壳上没有一道纹。

"没纹路的东西，成不了活。"这是镇上老驯兽师的原话。林昭记了七年，从他十岁那年蹲在人家门口偷听开始，一直到今天自己蹲在潭边，掌心托着这么个说不清来历的东西。

他把蛋揣进怀里，贴着心口那块最暖的地方。

——不管你是什么。先试试。

外面天光已经暗了。矿洞深处的黑和地上的黑不一样，地上的黑会散，这里的黑是一团一团压过来的，稠得像是没化开的墨。林昭提着灯往外走，走了两步又回头看了一眼那口水潭。

水面是平的，什么都没有。

可他后背发凉，像是有双眼睛刚才一直看着他把蛋拿走。

"别自己吓自己。"他低声说。

灯芯噼啪炸了一下。

他加快了脚步，右掌心那层洗不掉的矿灰在灯光下泛着一点点极淡的光 —— 他自己没看见。`

function buildChapters(novelId: number): Chapter[] {
  const arr: Chapter[] = []
  for (let i = 1; i <= 18; i++) {
    let status: Chapter['chapterStatus'] = 'OUTLINED'
    let content = ''
    let audit: Chapter['auditStatus'] = 'NOT_RUN'
    if (i <= 9) {
      status = 'APPROVED'
      audit = 'PASSED'
      content =
        i === 1
          ? CHAPTER1_SAMPLE
          : `（第 ${i} 章正文，共约 2500 字）\n\n这里是已入库的正文内容占位。`
    } else if (i === 10) {
      status = 'REVIEW_PENDING'
      audit = 'NEEDS_REVIEW'
      content = `（第 ${i} 章待审正文）\n\n${CHAPTER1_SAMPLE.slice(0, 600)}\n\n……AI 自检发现 2 处待确认问题，请在审核页处理。`
    }
    arr.push({
      id: novelId * 10000 + i,
      novelId,
      volumeId: novelId * 100 + 1,
      outlineId: (novelId * 100 + 1) * 1000 + i,
      chapterNo: i,
      title: OUTLINE_TITLES[i - 1] ?? `第 ${i} 章`,
      contentMd: content,
      wordCount: content ? 2400 + (i % 5) * 60 : 0,
      chapterStatus: status,
      auditStatus: audit,
      contentVersion: content ? 2 : 1,
      createdAt: NOW,
      updatedAt: NOW
    })
  }
  return arr
}

function buildIssues(): Issue[] {
  return [
    {
      id: 'i1',
      type: '越界',
      level: 'HIGH',
      quote: '老驯兽师说了一句"这纹路……不对，太素了"',
      issue: '该台词关联伏笔 #3 的提前解释，原计划第 6 章只埋不破，正文有半句被写成了解释性句子',
      suggest: '删掉"纹路太素意味着血脉未定"这半句，保留疑问语气即可',
      resolved: false
    },
    {
      id: 'i2',
      type: '设定冲突',
      level: 'MID',
      quote: '林昭从怀里摸出一张净息符',
      issue: '净息符设定 revealChapter = 7，本章为第 10 章属后续章的更高阶道具，但道具首次出现描写缺失来源说明',
      suggest: '补一句交代来源："这是上月镇上兑换来的"',
      resolved: false
    },
    {
      id: 'i3',
      type: '角色',
      level: 'LOW',
      quote: '灰羽发出一声低吼',
      issue: '灰羽幼生期尚未出现声带变化描写，低吼略超出当前阶段',
      suggest: '改为"从喉咙里挤出一声发哑的咯音"',
      resolved: false
    }
  ]
}

function buildChanges(): SettingChangeDraft[] {
  return [
    {
      id: 'c1',
      kind: 'CHARACTER_UPDATE',
      name: '灰羽',
      change: '能力：拟态 · 一阶段（可复制接触过的非生命物质质感，持续时间不超过十息）',
      accepted: true
    },
    {
      id: 'c2',
      kind: 'TIMELINE',
      name: '时间线',
      change: '林昭第一次带灰羽出现在公开场合',
      time: '天启历 982 年 · 腊月十九',
      accepted: true
    },
    {
      id: 'c3',
      kind: 'FORESHADOWING',
      name: '伏笔',
      change: '老驯兽师认出了蛋壳纹路，但没说',
      action: '埋',
      accepted: true
    },
    {
      id: 'c4',
      kind: 'NEW_ENTITY',
      name: '岩甲蟹',
      change: '野外常见一阶异兽，甲壳坚硬，弱点是腹甲接缝',
      entityType: 'CHARACTER',
      accepted: false
    }
  ]
}

const SCENES = [
  ['拆章纲', 0.42, 3],
  ['分段生成', 0.68, 12],
  ['一致性自检', 0.21, 10],
  ['摘要草稿', 0.18, 9],
  ['局部重写', 0.09, 4]
]

function buildUsage(): UsageRecord[] {
  const records: UsageRecord[] = []
  const models = ['deepseek-chat', 'deepseek-reasoner']
  let id = 1
  SCENES.forEach(([scene, cost, times]) => {
    for (let i = 0; i < (times as number); i++) {
      const inputTokens = 4200 + Math.round(Math.random() * 2600)
      const outputTokens = 700 + Math.round(Math.random() * 500)
      records.push({
        id: id++,
        jobId: `job-${1000 + id}`,
        scene: scene as string,
        model: models[id % 2],
        inputTokens,
        outputTokens,
        estimatedCost: Number((cost as number).toFixed(2)),
        createdAt: `2026-10-${String(20 + (id % 9)).padStart(2, '0')}T${String(
          9 + (id % 10)
        ).padStart(2, '0')}:${String((id * 7) % 60).padStart(2, '0')}:00`
      })
    }
  })
  return records.sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1))
}

// ---------------- 对外导出 ----------------
export const seed = {
  mainOutline: SAMPLE_MAIN_OUTLINE,
  volumeEvents: VOLUME_EVENTS,
  issues: buildIssues,
  changes: buildChanges
}

export function buildNovel(id: number, title: string, intro: string, gradient: string): Novel {
  return {
    id,
    userId: 1,
    title,
    intro,
    mainOutline: id === 1 ? SAMPLE_MAIN_OUTLINE : '',
    coverGradient: gradient,
    status: 'WRITING',
    version: 1,
    createdAt: NOW,
    updatedAt: NOW
  }
}

export const buildData = {
  volumes: buildVolumes,
  entities: buildEntities,
  worlds: buildWorlds,
  timeline: buildTimeline,
  foreshadowings: buildForeshadowings,
  style: buildStyle,
  outlines: buildOutlines,
  chapters: buildChapters,
  usage: buildUsage
}
