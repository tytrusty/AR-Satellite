/*
 * Almost an ordinary pass through vertex shader, but still accounts for position relative to light
 */
uniform mat4 u_ModelView;
uniform mat4 u_ModelViewProjection;

attribute vec4 a_Position;
attribute vec3 a_Normal;

varying vec3 v_ViewPosition;
varying vec3 v_ViewNormal;

void main() {
    v_ViewPosition = (u_ModelView * a_Position).xyz;
    v_ViewNormal = normalize((u_ModelView * vec4(a_Normal, 0.0)).xyz);
    gl_Position = u_ModelViewProjection * a_Position;
}
