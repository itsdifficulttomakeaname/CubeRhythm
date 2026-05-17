# TODOs

## 关于各朝向的渲染问题:
> 所有描述的均为测试中观察到的现象，(一般的)对描述取反即为你的修改操作

### 声明
- 从./resources/config.yml中可以获取字段 location 的值，表示玩家在开始游戏后被传送到的地方(不确定是代码加了修正还是游戏机制，传入的是整数坐标，但是玩家被修正到方块正中央，类似于(x.5,y,z.5)，不在乎这个，我们只需要使用直接从config获取的坐标值)
- 假定获取到Location:(x,y,z)
- 所有面向上都是Y+，向下都是Y-
- 对于W面，中心坐标为 (x, y+1, z+3)，向左为X+，向右为X-
- 对于A面，中心坐标为 (x+3, y+1, z)，向左为Z-，向右为Z+
- 对于S面，中心坐标为 (x, y+1, z-3)，向左为X-，向右为X+
- 对于D面，中心坐标为 (x-3, y+1, z)，向左为Z+，向右为Z-
- 对于下文提到的向左/向右，理解为面对判定面的向左或向右，和上面的描述对应即可

### 问题
```text
需要对每个音符做多标签支持，可能一个音符被多个不同的事件/执行器绑定，以避免单命名造成的兼容效果较差
整体上还有一个问题，观察到一个time: 3.0的音符达到判定面后立即执行了time: 4.0的execution音符，所以是不是说这个time: 3.0表示的不是在3.0s到达判定面，还是说另有他故
关于这个铺面测试出来的问题 @plugins/CubeRhythm/charts/event_test_full.json
1 [Combined 1] 旋转时，群组事件("tag": "spiral") 根据我的了解应该是想以(0,0)为中心轴半径为2的圆上移动、自转，但是有些音符绑定了这个事件，但是在事件完整运行完前就已经到达判定面了，导致落点可能和直接设定不同，所以可能需要预计算音符将落于何处，然后在对应位置渲染落点提示以及击中后的判定提示？
2 [Combined 2] 在缓动完全失效，音符都移到了(0,-2)处，而不是预期的(0,0)
3 [Combined 3] drop效果没有生效，音符直接按直线方向匀速靠近了
4 [Combined 4] drop效果没有生效，音符直接按直线方向匀速靠近了，叠加的event也完全无效
5 [Combined 5] 颜色有效，但是位置上的移动事件仍然无效
6 [Combined 6] 只有颜色有效，其他都无效
7 [Combined 7] 内联事件无效
8 [Combined 8] 内联事件无效
9 [Combined 9] 内联事件无效
关于这个铺面测试出来的问题 @plugins/CubeRhythm/charts/event_test_basic.json
1 X Y Z Alpha Scale Rotate Material这几个单通道绑定时(即每个音符的内联事件中只有上述六个中的一个)，均无效，Color通道单通道绑定生效没问题
2 多通道组合(X Scale Rotate Alpha Color)时Color生效，音符异常抽搐(卡在一个很小的角度抽搐的旋转)，其他均未生效
3 缓动对比(linear quadOut expoOut)意外的没有区别
4 hold 缓动无效
5 sineInOut (XY圆形轨迹) 无效
关于这个铺面测试出来的问题 @plugins/CubeRhythm/charts/event_test_group.json
1 wave_x 正常
2 scale (绑定为 "pulse" 的事件) 变大以后就没有变小了，和预期的脉冲效果不同
3 wave_y 似乎不正常，相较于wave_x 看不出明显的缓动轨迹
4 spin_fade alpha未生效，scale意外的放大了1倍，旋转看起来是正常的
5 timeRange 选择器有问题，也有可能是z方向的事件有问题，总之音符在28~34s仍然是线性流动的
6 tag: "rainbow" 正常生效
7 tag 选择器 似乎是正常生效的
```