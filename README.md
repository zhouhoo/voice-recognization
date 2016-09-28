# voice-recognizing
主题：nltk+htk+nodejs+php语音识别系统

v.0.9
客户端采用安卓app采集用户语音，然后上传到服务器，服务器端php处理客户端请求，识别语音后一方面送回app，另一方面，采用nodejs进行识别结果分发，
可以支持其他的客户端需求。
采用docker容器部署，redis容器作为数据交换媒介。
htk容器下载地址：http://pan.baidu.com/s/1c2KCDjy
nodejs容器下载地址：http://pan.baidu.com/s/1mhSTBwW
lmtools是语言模型训练工具，包括nltk，srilm。里面加入了一些自动化的脚本，方便使用。
其他详细情况见说明文档和代码。

版本：v.0.9
系统原理：
客户端采用安卓app采集用户语音，然后上传到服务器，服务器端php处理客户端请求，识别语音后一方面送回app，另一方面，采用nodejs的socket通讯库对识别结果分发，将结果数据推送到其他的客户端。
项目采用docker容器部署。
htk容器运行语音识别系统，容器镜像下载地址：http://pan.baidu.com/s/1hrXMJys
nodejs容器作为获取客户端请求，并且将语音识别结果分发出去。容器镜像下载地址：http://pan.baidu.com/s/1ctDEXK
redis容器作为数据交换媒介，安装的时候直接从公共镜像库安装最新的即可。

Andoroid 文件夹下是app客户端代码，初始版本，界面有点丑。

附加资料：
1. 语音识别系统架构文档.docx是系统详细说明书。
2.nltk是斯坦福的一个NLP库，可以实现中文自动分词， 我基于这个实现了分词，分词验证，拼音标注等功能。 srilm是一个语言模型训练工具，讲训练好的语言模型输入给htk。声音模型我采用的是导师另一个项目训练好的模型。 
3. 所有的htk前期语言预处理的工具我都打包在lmtools压缩包里。压缩包还包括了整个系统的使用说明，一些自动化的脚本文件。下载链接：http://pan.baidu.com/s/1dEB8l37

初始版本，欢迎修正，交流。

