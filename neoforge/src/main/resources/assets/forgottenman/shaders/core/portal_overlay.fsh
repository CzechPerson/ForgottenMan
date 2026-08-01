#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

void main() {
    // Faint scanline shimmer over the portal geometry
    float n = hash(vec2(floor(gl_FragCoord.y / 2.0), floor(GameTime * 24000.0)));
    fragColor = vec4(vec3(n), 0.06) * ColorModulator;
}
