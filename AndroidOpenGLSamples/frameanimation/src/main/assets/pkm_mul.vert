uniform mat4 uMVPMatrix;//总变换矩阵
attribute vec4 aPosition;//顶点位置
attribute vec2 aTexCoord;//顶点纹理坐标
varying vec2 vTextureCoord;//用于传递给片元着色器的变量

void main(){
    vTextureCoord = aTexCoord;//将接收的纹理坐标传递给片元着色器
    gl_Position = uMVPMatrix * aPosition;//根据总变换矩阵计算此次绘制此顶点位置
}