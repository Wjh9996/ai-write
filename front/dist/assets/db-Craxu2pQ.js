var S=Object.defineProperty;var R=(t,a,e)=>a in t?S(t,a,{enumerable:!0,configurable:!0,writable:!0,value:e}):t[a]=e;var N=(t,a,e)=>R(t,typeof a!="symbol"?a+"":a,e);const T="/api",p="ai_write_token";class D extends Error{constructor(e,s){super(s);N(this,"code");this.code=e,this.name="ApiError"}}const h={};function m(){try{return typeof localStorage<"u"?localStorage:null}catch{return null}}function M(){var t;return((t=m())==null?void 0:t.getItem(p))??h[p]??""}function j(t){var a;h[p]=t,(a=m())==null||a.setItem(p,t)}function K(){var t;delete h[p],(t=m())==null||t.removeItem(p)}async function Y(t,a,e,s){{if(!s)throw new D(500,`未提供 mock 实现：${t} ${a}`);return await s()}}function B(t){const{path:a,lastEventId:e,onEvent:s,onError:n,mockFactory:o}=t;if(o){let d=!1;const C=o(I=>{d||s==null||s(I)},()=>{d=!0});return{close:()=>{d=!0,C()}}}const l=a.includes("?")?"&":"?",r=`${T}${a}${l}token=${encodeURIComponent(M())}`+(e?`&lastEventId=${e}`:""),c=new EventSource(r);return c.onmessage=d=>{try{s==null||s(JSON.parse(d.data))}catch(C){n==null||n(C)}},c.onerror=d=>{n==null||n(d)},{close:()=>c.close()}}const u="2026-10-31T10:00:00",g=`【主线】开局在废矿捡到一枚灰蛋 → 孵化出第一只兽宠 → 参加宝宝赛 → 晋升高级学府 → 学府大比 → 世界裂缝扩散，异兽袭击 → 战场生死危机 → 战异兽皇 → 最终之战 → 成为扬名世界的御兽大师。

【第一卷·废矿】主角林昭在家族废弃矿洞里发现一枚无人认领的灰蛋，靠半吊子驯兽知识把它孵出来，带着这只奇怪的小东西走出矿镇，第一次接触真正的御兽世界。

【第二卷·宝宝赛】初入低级学府测试场，以"灰羽"参加全国宝宝赛，靠一次近乎犯规的临场判断拿下名次，也因此被高级学府的招生官盯上。

【第三卷·学府】进入青崖高级学府，加入 Eh?……加入"杂班"，遭遇旧势力排挤，结识挚友与宿敌，学分赛上一路逆袭。

【第四卷·裂缝】世界裂缝扩散，异兽开始袭击人类聚居地，主角第一次上战场，在生死边缘完成第二次契约。

【第五卷·异兽皇】战线崩溃，异兽皇现世，主角团付出惨重代价守住断后线，挚友重伤，主角觉醒第三契约。

【第六卷·终战】集结全部伙伴发动总攻，在 worlds 尽头击杀异兽皇，成为扬名世界的御兽大师。

【第七卷·余响】战后重建，主角回到废矿，把那枚蛋壳埋在原处，收了一个和自己当年一样穷的学徒。`,A={1:["林昭被族里发配去看守废弃矿洞","在矿洞深处发现一枚无人认领的灰蛋","尝试孵化失败三次，差点被自己冻死","用体温和最后一袋灵米把蛋孵了出来",'幼兽破壳，认主，取名"灰羽"',"带灰羽第一次走出矿镇，被镇上嘲笑","遭遇第一只野生岩甲蟹，被迫实战","矿镇小比，赢下第一枚灵石","卷末钩子：招生官的车停在了矿镇口"],2:["抵达低级学府测试场，资质评定垫底","报名全国宝宝赛，被判定为送分名额","首轮对战第一学府的天才","临场判断：让灰羽假死骗过对手的预判","爆冷晋级十六强","八强赛被针对，灰羽受伤","决赛以微弱差距落败，拿到第二","青崖高级学府招生官当场递出特招令"],3:['入学青崖，被分进被称为"垃圾场"的丙字班',"认识同桌苏晚与宿敌裴临","第一次学分赛，丙字班垫底",'发现灰羽的"拟态"能力隐藏的第二阶段',"中期考核逆袭，进入甲字班视野","期末大比，苏晚为救林昭受伤","林昭以第七名收官，获得进入禁地试炼资格"]};function O(t){return[{id:t*100+1,novelId:t,volumeNo:1,title:"第一卷 · 废矿里的灰蛋",events:A[1].map((a,e)=>({id:11+e,sort:e+1,content:a})),status:"CONFIRMED",targetChapters:18,createdAt:u},{id:t*100+2,novelId:t,volumeNo:2,title:"第二卷 · 宝宝赛",events:A[2].map((a,e)=>({id:21+e,sort:e+1,content:a})),status:"PENDING_CONFIRM",targetChapters:16,createdAt:u},{id:t*100+3,novelId:t,volumeNo:3,title:"第三卷 · 青崖学府",events:A[3].map((a,e)=>({id:31+e,sort:e+1,content:a})),status:"RAW",targetChapters:20,createdAt:u}]}function y(t){return[{id:1,novelId:t,type:"CHARACTER",name:"林昭",aliases:["主角","小昭"],card:{身份:"旁系子弟，被发配守废矿",年龄:"16",性格:"闷，认死理，一旦认准就不松口",外貌:"瘦，黑，右手掌心有一层洗不掉的矿灰",口癖:'"……先试试。"',当前等级:"御兽学徒 · 一阶",契约兽:"灰羽"},source:"MANUAL",approvalStatus:"CONFIRMED",revealChapter:1,updatedAt:u},{id:2,novelId:t,type:"CHARACTER",name:"灰羽",aliases:["小东西","灰团子"],card:{身份:"林昭的第一只契约兽",种族:"未知（蛋壳无纹）",阶段:"幼生期",能力:"拟态 · 一阶段：复制接触过的非生命物质质感",口癖:'"啾。"',禁忌:"不得提前解释其血脉来源"},source:"AI",approvalStatus:"CONFIRMED",revealChapter:5,updatedAt:u},{id:3,novelId:t,type:"CHARACTER",name:"苏晚",aliases:["同桌"],card:{身份:"青崖丙字班，后来转甲班",性格:"嘴毒心软",契约兽:"素心螟",备注:"第三卷末为救林昭受伤"},source:"AI",approvalStatus:"DRAFT",revealChapter:20,updatedAt:u},{id:4,novelId:t,type:"CHARACTER",name:"裴临",aliases:[],card:{身份:"甲字班首席，裴家嫡子",关系:"宿敌，后来是战友",契约兽:"赤鳞蛟"},source:"AI",approvalStatus:"DRAFT",revealChapter:22,updatedAt:u},{id:5,novelId:t,type:"PLACE",name:"黑石废矿",aliases:["废矿"],card:{位置:"林家领地最西端，靠着断崖",特点:"矿脉枯竭，常年渗水，洞壁有不明抓痕",备注:"灰蛋发现地点：三号巷道尽头的水潭边"},source:"MANUAL",approvalStatus:"CONFIRMED",revealChapter:1,updatedAt:u},{id:6,novelId:t,type:"ITEM",name:"净息符",aliases:[],card:{作用:"压制灵力波动",备注:"第七章才能出现，第一章严禁提及"},source:"MANUAL",approvalStatus:"CONFIRMED",revealChapter:7,updatedAt:u}]}function b(t){return[{id:1,novelId:t,category:"世界规则",title:"契约规则",content:"契约不可逆。契约成立的瞬间，御兽师与兽宠共享部分感知，也共担伤害。契约兽死亡，御兽师轻则废掉半身灵脉，重则当场毙命。",source:"MANUAL",approvalStatus:"CONFIRMED"},{id:2,novelId:t,category:"世界规则",title:"境界划分",content:"学徒 → 御者 → 御师 → 大御师 → 御王 → 御皇。每一阶分九段。",source:"MANUAL",approvalStatus:"CONFIRMED"},{id:3,novelId:t,category:"地理",title:"世界裂缝",content:"位于大陆北端的巨型裂隙，异兽潮的源头。第四卷开始扩散。当前阶段普通人不知道裂缝的存在。",source:"AI",approvalStatus:"CONFIRMED"},{id:4,novelId:t,category:"势力",title:"青崖高级学府",content:'东境最高学府，分甲乙丙三班。丙字班俗称"垃圾场"。',source:"AI",approvalStatus:"DRAFT"}]}function F(t){return[{id:1,novelId:t,chapterId:1,timeLabel:"天启历 982 年 · 冬月末",event:"林昭被发配看守黑石废矿",approvalStatus:"CONFIRMED",sort:1},{id:2,novelId:t,chapterId:5,timeLabel:"天启历 982 年 · 腊月初",event:"灰羽破壳并认主",approvalStatus:"CONFIRMED",sort:2},{id:3,novelId:t,chapterId:9,timeLabel:"天启历 982 年 · 腊月十七",event:"第一次实战，击退野生岩甲蟹",approvalStatus:"CONFIRMED",sort:3},{id:4,novelId:t,chapterId:18,timeLabel:"天启历 983 年 · 正月",event:"青崖招生官抵达矿镇",approvalStatus:"CONFIRMED",sort:4}]}function L(t){return[{id:1,novelId:t,content:"三号巷道尽头的洞壁上有五道抓痕，比人手宽一倍",setChapterId:2,payoffChapterId:null,status:"PLANTED",source:"MANUAL",approvalStatus:"CONFIRMED"},{id:2,novelId:t,content:"林昭掌心洗不掉的灰 —— 后来证明是蛋壳粉末渗进皮肤",setChapterId:1,payoffChapterId:null,status:"PLANTED",source:"MANUAL",approvalStatus:"CONFIRMED"},{id:3,novelId:t,content:'镇上老驯兽师说了一句"这纹路……不对，太素了"',setChapterId:6,payoffChapterId:null,status:"PLANTED",source:"AI",approvalStatus:"DRAFT"}]}function P(t){return{id:1,novelId:t,card:{人称:"第三人称",视角:"限知视角（跟随林昭）",节奏:"两章铺垫一章爆发，每章至少一个钩子",基调:"正能量、积极向上，但要有重量，不廉价的热血",对话比例:"30%—35%",句长:"短句为主，动作场景用极短句",爽点:"以弱胜强、被轻视后的正名、伙伴羁绊",禁忌:"不写虐心虐身；不写暧昧多女主；不出现现代词汇"},sampleText:`矿洞里的水一直在滴。

林昭数到第三百二十七滴的时候，掌心底下那枚蛋动了一下。

很轻，像有人在里头翻了个身。

他屏住呼吸，把耳朵贴上去。里面静了半晌，又动了一下。

"……活的。"

他说出这两个字的时候，嗓子哑得不像自己的。`,forbiddenTerms:["上帝视角","系统","金手指","女主"]}}const v=["废矿里的灰蛋","三号巷道","掌心里的温度","第三次失败","破壳","啾","老驯兽师的话","走出矿镇","岩甲蟹","十四枚灵石"];function $(t){const a=t*100+1,e=[["林昭在废矿清点工具","发现巷道深处的反光","决定独自下到三号巷道尽头"],["描述三号巷道的渗水与寂静","水潭边那枚灰蛋第一次出现","林昭伸手拿起来时的触感"],["回工棚查资料","确认这不是任何已知兽卵","决定试着孵化"],["第一次孵化失败，蛋变冷","第二次失败","林昭把最后半袋灵米碾成粉"],["第三次尝试，用体温捂了一整夜","蛋壳出现第一道裂纹","林昭昏睡过去"],["破壳瞬间，一只湿漉漉的幼兽","对视，认主契约成形","林昭给它取名灰羽"],["带灰羽去见镇上的老驯兽师",'老驯兽师看了一眼蛋壳说"纹路太素"',"拒绝解释含义，埋下钩子"],["林昭带灰羽走出矿镇","被同龄人嘲笑捡了个废品","灰羽第一次展露拟态"],["回程路上遭遇野生岩甲蟹","灰羽受伤，林昭以肉身挡下一次钳击","用地形反杀"],["矿镇小比报名","连胜三轮","赢下第一枚灵石，决定离开矿镇"]];return v.map((s,n)=>({id:a*1e3+n+1,volumeId:a,chapterNo:n+1,title:`${n+1}. ${s}`,beats:e[n]??["推进本卷主线的关键一步","与已有角色互动","章末留下钩子"],characters:n===0?["林昭"]:["林昭","灰羽"],estimatedWords:2500,status:n<9?"CONFIRMED":"DRAFT",version:1}))}const f=`矿洞里的水一直在滴。

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

他加快了脚步，右掌心那层洗不掉的矿灰在灯光下泛着一点点极淡的光 —— 他自己没看见。`;function w(t){const a=[];for(let e=1;e<=18;e++){let s="OUTLINED",n="",o="NOT_RUN";e<=9?(s="APPROVED",o="PASSED",n=e===1?f:`（第 ${e} 章正文，共约 2500 字）

这里是已入库的正文内容占位。`):e===10&&(s="REVIEW_PENDING",o="NEEDS_REVIEW",n=`（第 ${e} 章待审正文）

${f.slice(0,600)}

……AI 自检发现 2 处待确认问题，请在审核页处理。`),a.push({id:t*1e4+e,novelId:t,volumeId:t*100+1,outlineId:(t*100+1)*1e3+e,chapterNo:e,title:v[e-1]??`第 ${e} 章`,contentMd:n,wordCount:n?2400+e%5*60:0,chapterStatus:s,auditStatus:o,contentVersion:n?2:1,createdAt:u,updatedAt:u})}return a}function _(){return[{id:"i1",type:"越界",level:"HIGH",quote:'老驯兽师说了一句"这纹路……不对，太素了"',issue:"该台词关联伏笔 #3 的提前解释，原计划第 6 章只埋不破，正文有半句被写成了解释性句子",suggest:'删掉"纹路太素意味着血脉未定"这半句，保留疑问语气即可',resolved:!1},{id:"i2",type:"设定冲突",level:"MID",quote:"林昭从怀里摸出一张净息符",issue:"净息符设定 revealChapter = 7，本章为第 10 章属后续章的更高阶道具，但道具首次出现描写缺失来源说明",suggest:'补一句交代来源："这是上月镇上兑换来的"',resolved:!1},{id:"i3",type:"角色",level:"LOW",quote:"灰羽发出一声低吼",issue:"灰羽幼生期尚未出现声带变化描写，低吼略超出当前阶段",suggest:'改为"从喉咙里挤出一声发哑的咯音"',resolved:!1}]}function U(){return[{id:"c1",kind:"CHARACTER_UPDATE",name:"灰羽",change:"能力：拟态 · 一阶段（可复制接触过的非生命物质质感，持续时间不超过十息）",accepted:!0},{id:"c2",kind:"TIMELINE",name:"时间线",change:"林昭第一次带灰羽出现在公开场合",time:"天启历 982 年 · 腊月十九",accepted:!0},{id:"c3",kind:"FORESHADOWING",name:"伏笔",change:"老驯兽师认出了蛋壳纹路，但没说",action:"埋",accepted:!0},{id:"c4",kind:"NEW_ENTITY",name:"岩甲蟹",change:"野外常见一阶异兽，甲壳坚硬，弱点是腹甲接缝",entityType:"CHARACTER",accepted:!1}]}const W=[["拆章纲",.42,3],["分段生成",.68,12],["一致性自检",.21,10],["摘要草稿",.18,9],["局部重写",.09,4]];function k(){const t=[],a=["deepseek-chat","deepseek-reasoner"];let e=1;return W.forEach(([s,n,o])=>{for(let l=0;l<o;l++){const r=4200+Math.round(Math.random()*2600),c=700+Math.round(Math.random()*500);t.push({id:e++,jobId:`job-${1e3+e}`,scene:s,model:a[e%2],inputTokens:r,outputTokens:c,estimatedCost:Number(n.toFixed(2)),createdAt:`2026-10-${String(20+e%9).padStart(2,"0")}T${String(9+e%10).padStart(2,"0")}:${String(e*7%60).padStart(2,"0")}:00`})}}),t.sort((s,n)=>s.createdAt<n.createdAt?1:-1)}const J={mainOutline:g,volumeEvents:A,issues:_,changes:U};function E(t,a,e,s){return{id:t,userId:1,title:a,intro:e,mainOutline:t===1?g:"",coverGradient:s,status:"WRITING",version:1,createdAt:u,updatedAt:u}}const i={volumes:O,entities:y,worlds:b,timeline:F,foreshadowings:L,style:P,outlines:$,chapters:w,usage:k},G="linear-gradient(135deg,#6366f1 0%,#a855f7 45%,#ec4899 100%)",H="linear-gradient(135deg,#38bdf8 0%,#6366f1 100%)",V="linear-gradient(135deg,#34d399 0%,#22d3ee 100%)";function x(){const t=E(1,"御兽：从一枚灰蛋开始","讲述一个被发配守废矿的少年，从一枚灰蛋开始成为御兽大师的故事。",G),a=E(2,"旧城剑客事务所","现代都市背景下的剑客工会，接委托、查真相、守规矩。",H),e=E(3,"虫巢纪元","人类最后一座城，被虫群围了七十年。",V),s={novels:[t,a,e],volumes:[...i.volumes(1),...i.volumes(2),...i.volumes(3)],outlines:[...i.outlines(1),...i.outlines(2),...i.outlines(3)],chapters:i.chapters(1),entities:i.entities(1),worlds:i.worlds(1),usage:i.usage(),style:{1:i.style(1)}};return s.novels.forEach(n=>{const o=s.chapters.filter(r=>r.novelId===n.id),l=o.reduce((r,c)=>r+c.wordCount,0);n.stats={totalWords:l||(n.id===1?24e3:0),chapterCount:o.length||18,approvedCount:o.filter(r=>r.chapterStatus==="APPROVED").length,reviewPendingCount:o.filter(r=>r.chapterStatus==="REVIEW_PENDING").length,generatingCount:o.filter(r=>r.chapterStatus==="GENERATING").length,outlinedCount:18,volumeCount:s.volumes.filter(r=>r.novelId===n.id).length}}),s.novels[1].stats={totalWords:12400,chapterCount:6,approvedCount:4,reviewPendingCount:1,generatingCount:0,outlinedCount:12,volumeCount:3},s.novels[2].stats={totalWords:3100,chapterCount:2,approvedCount:1,reviewPendingCount:0,generatingCount:0,outlinedCount:8,volumeCount:2},s}const z=x();function Q(t,a=380){return new Promise(e=>setTimeout(()=>e(structuredClone(t)),a))}function X(t=300){return new Promise(a=>setTimeout(()=>a(null),t))}function Z(t="id"){return`${t}_${Math.random().toString(36).slice(2,9)}`}export{K as a,Q as b,Y as c,X as d,z as e,J as f,M as g,i as h,B as o,j as s,Z as u};
