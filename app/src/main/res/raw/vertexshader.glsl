// matrice per muovere il vertice
uniform mat4 uMVPMatrix;
// posizioe del vertice...
attribute vec4 vPosition;
// ... e relativo punto nella texture...
attribute vec2 a_texCoord;
// ...che verrà passato al fragment shader
varying vec2 v_texCoord;
void main() {
    // muove il vertice servendosi della matrice uMVPMatrix
    gl_Position = uMVPMatrix * vPosition;
    // fa sapere al fragment che punto nella texture va usato per questo vertice
    v_texCoord = a_texCoord;
}