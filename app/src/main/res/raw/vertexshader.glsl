/* This matrix member variable provides a hook to manipulate
the coordinates of the objects that use this vertex shader*/
uniform mat4 uMVPMatrix;
attribute vec4 vPosition;
/*texture location as input*/
attribute vec2 a_texCoord;
/*...and gives it to the fragment shader */
varying vec2 v_texCoord;
void main() {
    /*
    The matrix must be included as a modifier of gl_Position.
    Note that the uMVPMatrix factor *must be first* in order
    for the matrix multiplication product to be correct.
    */
    gl_Position = uMVPMatrix * vPosition;
    v_texCoord = a_texCoord;
}