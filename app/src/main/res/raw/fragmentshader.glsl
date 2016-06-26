precision mediump float;
//coordinate della texture da cui prelevare il colore
varying vec2 v_texCoord;
//texture
uniform sampler2D s_texture;
//colore da sommare a quello della texture
uniform vec4 vColor;
void main() {
    //colora il fragment
    gl_FragColor = vColor * texture2D( s_texture, v_texCoord);
}