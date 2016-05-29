precision mediump float;
varying vec2 v_texCoord;
uniform sampler2D s_texture;
uniform vec4 vColor;
void main() {
    gl_FragColor = vColor * texture2D( s_texture, v_texCoord);
}