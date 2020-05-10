precision mediump float;
varying vec2 vTextureCoord;//接收从顶点着色器过来的参数
uniform sampler2D sTexture;//纹理内容数据
//uniform sampler2D sTextureAlpha;//纹理内容数据

void main() {
    vec4 color = texture2D(sTexture, vTextureCoord);
//    color.a = texture2D(sTextureAlpha, vTextureCoord).r;
    gl_FragColor = color;
}