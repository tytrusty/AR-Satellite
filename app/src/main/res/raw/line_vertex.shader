/*
 * Simple vertex shader that computes vertex position with MVP matrix
 * and also passes through the world position of the vertex
 */
uniform mat4 u_ModelViewProjection;

attribute vec4 a_Position;

varying vec3 v_WorldPosition;

void main() {
    // Simple pass through world position
    v_WorldPosition = a_Position.xyz;

    // position of vertex, in clip space
    gl_Position = u_ModelViewProjection * a_Position;
}
