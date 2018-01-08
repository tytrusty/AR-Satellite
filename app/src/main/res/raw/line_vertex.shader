/*
 * Simple vertex shader that computes vertex position with MVP matrix
 * and also passes through the model position of the vertex
 */
uniform mat4 u_ModelViewProjection;

attribute vec4 a_Position;

varying vec3 v_LocalPosition;

void main() {
    // Simple pass through model coordinates
    v_LocalPosition = a_Position.xyz;

    // position of vertex, in clip space
    gl_Position = u_ModelViewProjection * a_Position;
}
