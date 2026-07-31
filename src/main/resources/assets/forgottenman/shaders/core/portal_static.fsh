#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

void main() {
    // Dark red-black static for doorways with no live feed
    vec2 cell = floor(gl_FragCoord.xy / 3.0);
    float n = hash(cell + floor(GameTime * 24000.0));
    vec3 color = mix(vec3(0.02, 0.0, 0.01), vec3(0.32, 0.05, 0.08), n * n);
    fragColor = vec4(color, 1.0) * ColorModulator;
}
