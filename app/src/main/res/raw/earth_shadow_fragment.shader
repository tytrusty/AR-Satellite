/**
 * Fragment shader to create a basic drop shadow for the earth object
 */
precision mediump float;

uniform vec2 u_Origin;
uniform vec4 u_Color;
uniform float u_Height; // ranges from [0, 1]

varying vec3 v_LocalPosition;

void main() {
    // The scalars are just magic numbers that I tinkered with to make the results
    // more appealing.
    float dist = clamp(distance(u_Origin.xy, v_LocalPosition.xz) * 1.2, 0.0, 1.0);
    float alpha = clamp( (1.0 - dist) / (u_Height * 1.5), 0.0, 0.7);
    gl_FragColor = vec4(u_Color.xyz, alpha);
}
