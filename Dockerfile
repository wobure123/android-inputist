# 使用官方Android构建环境
FROM openjdk:17-jdk-slim

# 安装必要的工具
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    wget \
    git \
    && rm -rf /var/lib/apt/lists/*

# 设置Android SDK环境变量
ENV ANDROID_HOME /opt/android-sdk
ENV PATH ${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# 创建SDK目录
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools

# 下载并安装Android命令行工具
WORKDIR /tmp
RUN wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip && \
    unzip commandlinetools-linux-11076708_latest.zip && \
    mv cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm commandlinetools-linux-11076708_latest.zip

# 接受SDK许可证并安装必要组件
RUN yes | ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager --licenses && \
    ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0" \
    "platforms;android-33" \
    "build-tools;33.0.2"

# 设置工作目录
WORKDIR /workspace

# 复制项目文件
COPY . .

# 构建APK
RUN ./gradlew assembleDebug --stacktrace

# 输出APK位置
RUN ls -la app/build/outputs/apk/debug/
