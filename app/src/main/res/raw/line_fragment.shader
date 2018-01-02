/**
 * Fragment shader to create a dotted line.
 */
precision mediump float;

uniform vec2 u_Origin;
uniform vec4 u_Color;

varying vec3 v_WorldPosition;

// Bias applied to make the spaces and dashes unequal size
float bias = 0.5;

// Scalar to increase dotted frequency
float frequency = 200.0;

void main() {
    if (cos(frequency * abs(distance(u_Origin.xy, v_WorldPosition.xy))) + bias > 0.0) {
        gl_FragColor = u_Color;
    } else {
        gl_FragColor = vec4(0, 0, 0, 0);
    }
}
